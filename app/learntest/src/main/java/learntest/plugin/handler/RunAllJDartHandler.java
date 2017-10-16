package learntest.plugin.handler;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import learntest.core.LearnTestParams;
import learntest.core.RunTimeInfo;
import learntest.core.commons.LearntestConstants;
import learntest.core.commons.data.LearnTestApproach;
import learntest.core.commons.data.classinfo.MethodInfo;
import learntest.core.commons.data.classinfo.TargetMethod;
import learntest.local.DetailExcelReader;
import learntest.local.DetailTrial;
import learntest.local.MethodTrial;
import learntest.plugin.LearnTestConfig;
import learntest.plugin.ProjectSetting;
import learntest.plugin.export.io.excel.MultiTrial;
import learntest.plugin.export.io.excel.Trial;
import learntest.plugin.export.io.excel.TrialExcelHandler;
import learntest.plugin.export.io.excel.TrialExcelReader;
import learntest.plugin.handler.filter.classfilter.ClassNameFilter;
import learntest.plugin.handler.filter.classfilter.ITypeFilter;
import learntest.plugin.handler.filter.classfilter.TestableClassFilter;
import learntest.plugin.handler.filter.methodfilter.IMethodFilter;
import learntest.plugin.handler.filter.methodfilter.MethodNameFilter;
import learntest.plugin.handler.filter.methodfilter.NestedBlockChecker;
import learntest.plugin.handler.filter.methodfilter.TestableMethodFilter;
import learntest.plugin.utils.IMethodUtils;
import learntest.plugin.utils.IProjectUtils;
import learntest.plugin.utils.IStatusUtils;
import learntest.plugin.utils.LearnTestUtil;
import sav.common.core.SavRtException;
import sav.common.core.utils.FileUtils;
import sav.common.core.utils.SingleTimer;
import sav.settings.SAVTimer;

public class RunAllJDartHandler extends AbstractLearntestHandler {

	private static Logger log = LoggerFactory.getLogger(EvaluationHandler.class);
	private static final int EVALUATIONS_PER_METHOD = 5;
	private List<IMethodFilter> methodFilters;
	private List<ITypeFilter> classFilters;
	private static String SUCCESSFUL_FILE_NAME = "jdart_" + LearntestConstants.EXCLUSIVE_METHOD_FILE_NAME;

	private int curMethodIdx = 0;

	@Override
	protected IStatus execute(IProgressMonitor monitor) {

		SingleTimer timer = SingleTimer.start("Evaluation all methods");
		curMethodIdx = 0;
		String projectName = LearnTestConfig.getINSTANCE().getProjectName();
		final List<IPackageFragmentRoot> roots = IProjectUtils
				.getSourcePkgRoots(IProjectUtils.getJavaProject(projectName));
		TrialExcelHandler excelHandler = null;
		try {
			String outputFolder = ProjectSetting.getLearntestOutputFolder(projectName);
			log.info("learntest output folder: {}", outputFolder);
			excelHandler = new TrialExcelHandler(outputFolder, projectName + "_jdart");
			initFilters();
		} catch (Exception e1) {
			handleException(e1);
			return Status.CANCEL_STATUS;
		}
		SAVTimer.enableExecutionTimeout = true;
		SAVTimer.exeuctionTimeout = 300000;
		RunTimeCananicalInfo overalInfo = new RunTimeCananicalInfo(0, 0, 0);
		try {
			for (IPackageFragmentRoot root : roots) {
				for (IJavaElement element : root.getChildren()) {
					if (element instanceof IPackageFragment) {
						RunTimeCananicalInfo info = runEvaluation((IPackageFragment) element, excelHandler, monitor);
						overalInfo.add(info);
					}
				}
			}
			log.info(overalInfo.toString());
		} catch (JavaModelException e) {
			handleException(e);
		} catch (OperationCanceledException e) {
			timer.logResults(log);
			log.info(e.getMessage());
			return IStatusUtils.cancel();
		}
		timer.logResults(log);
		return Status.OK_STATUS;
	}

	@Override
	protected String getJobName() {
		return "Run JDart for All Methods";
	}

	private void initFilters() {
		methodFilters = Arrays.asList(new TestableMethodFilter(), new NestedBlockChecker(),
				new MethodNameFilter(SUCCESSFUL_FILE_NAME),
				new MethodNameFilter(LearntestConstants.SKIP_METHOD_FILE_NAME));
		classFilters = Arrays.asList(new TestableClassFilter(), new ClassNameFilter(getExcludedClasses()));
	}

	private List<String> getExcludedClasses() {
		/* TODO - temporary hard code */
		return Arrays.asList("org.apache.tools.ant.Main");
	}

	private RunTimeCananicalInfo runEvaluation(IPackageFragment pkg, TrialExcelHandler excelHandler,
			IProgressMonitor monitor) throws JavaModelException {
		RunTimeCananicalInfo info = new RunTimeCananicalInfo();
		for (IJavaElement javaElement : pkg.getChildren()) {
			if (javaElement instanceof IPackageFragment) {
				runEvaluation((IPackageFragment) javaElement, excelHandler, monitor);
			} else if (javaElement instanceof ICompilationUnit) {
				ICompilationUnit icu = (ICompilationUnit) javaElement;
				CompilationUnit cu = LearnTestUtil.convertICompilationUnitToASTNode(icu);
				boolean valid = true;
				for (ITypeFilter classFilter : classFilters) {
					if (!classFilter.isValid(cu)) {
						valid = false;
						continue;
					}
				}
				if (!valid) {
					continue;
				}
				TestableMethodCollector collector = new TestableMethodCollector(cu, methodFilters);
				cu.accept(collector);
				List<MethodInfo> validMethods = collector.getValidMethods();
				updateRuntimeInfo(info, cu, collector.getTotalMethodNum(), validMethods.size());
				evaluateForMethodList(excelHandler, validMethods, monitor);
			}
		}
		return info;
	}

	private void updateRuntimeInfo(RunTimeCananicalInfo info, CompilationUnit cu, int totalMethods, int validMethods) {
		int length0 = cu.getLineNumber(cu.getStartPosition() + cu.getLength() - 1);
		info.addTotalLen(length0);
		info.validNum += validMethods;
		info.totalNum += totalMethods;
	}

	protected void evaluateForMethodList(TrialExcelHandler excelHandler, List<MethodInfo> targetMethods,
			IProgressMonitor monitor) {
		if (targetMethods.isEmpty()) {
			return;
		}
		for (MethodInfo targetMethod : targetMethods) {
			/* todo : test special method start */
//			if (skip(targetMethod)) {
//				continue;
//			}
			/* todo : test special method end */

			log.info("-----------------------------------------------------------------------------------------------");
			log.info("Method {}", ++curMethodIdx);

			MultiTrial multiTrial = new MultiTrial();
			for (int i = 0; i < EVALUATIONS_PER_METHOD; i++) {
				checkJobCancelation(monitor);
				try {
					LearnTestParams params = initLearntestParams(targetMethod);
					Trial trial = evaluateLearntestForSingleMethod(params);
					if (trial != null) {
						multiTrial.addTrial(trial);
					}
				} catch (Exception e) {
					handleException(e);
				}
			}
			multiTrial.setAvgInfo();
			if (!multiTrial.isEmpty()) {
				try {
					excelHandler.export(multiTrial);
				} catch (Exception e) {
					handleException(e);
				}
			}
			logSuccessfulMethod(targetMethod);
		}
	}

	Map<String, MethodTrial> oldTrials = null;
	private boolean skip(MethodInfo targetMethod) {
		if (oldTrials == null) {
			try {
				DetailExcelReader reader = new DetailExcelReader(new File("D:/eclipse/apache-common-math-2.2_0-checked.xlsx"));
				List<MethodTrial> list = reader.readDataSheet();
				oldTrials = new HashMap<>();
				for (MethodTrial methodTrial : list) {
					String name = methodTrial.getMethodName();
					int line = methodTrial.getLine();
					oldTrials.put(name+"_"+line, methodTrial);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String fullName = targetMethod.getMethodFullName();
		int line = targetMethod.getLineNum();
		log.debug(fullName);
		if (oldTrials.containsKey(fullName + "_" + line)) {
			MethodTrial trial = oldTrials.get(fullName + "_" + line);
			for (DetailTrial detail : trial.getTrials()) {
				if ((detail.getL2tBetter().length() > 0)
						|| (detail.getRanBetter().length() > 0)) {
					return false;
				}
			}
		}
		return true;
	}

	private void checkJobCancelation(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException("Operation cancelled!");
		}
	}

	private static void logSuccessfulMethod(MethodInfo targetMethod) {
		try {
			FileUtils.appendFile(SUCCESSFUL_FILE_NAME,
					IMethodUtils.getMethodId(targetMethod.getMethodFullName(), targetMethod.getLineNum()) + "\n");
		} catch (SavRtException e) {
			// ignore
		}
	}

	private LearnTestParams initLearntestParams(MethodInfo targetMethod) throws CoreException {
		LearnTestParams params = new LearnTestParams(getAppClasspath(), targetMethod);
		setSystemConfig(params);
		return params;
	}

	@Override
	protected Trial evaluateLearntestForSingleMethod(LearnTestParams params) {
		try {

			log.info("");
			log.info("WORKING METHOD: {}, line {}", params.getTargetMethod().getMethodFullName(),
					params.getTargetMethod().getLineNum());
			log.info("-----------------------------------------------------------------------------------------------");

			LearnTestParams randoopParam = params.createNew();

			RunTimeInfo l2tAverageInfo = new RunTimeInfo();
			RunTimeInfo ranAverageInfo = new RunTimeInfo();
			RunTimeInfo jdartInfo = null;

			randoopParam.setApproach(LearnTestApproach.RANDOOP);
			log.info("run jdart..");
			jdartInfo = runJdart(randoopParam);

			TargetMethod method = params.getTargetMethod();
			log.info("Result: ");
			log.info("jdart: {}", jdartInfo);
			return new Trial(method.getMethodFullName(), method.getMethodLength(), method.getLineNum(), l2tAverageInfo,
					ranAverageInfo, jdartInfo);
		} catch (Exception e) {
			handleException(e);
		}
		return null;
	}

	class RunTimeCananicalInfo {
		int validNum;
		int totalNum;
		int totalLen;

		public RunTimeCananicalInfo() {

		}

		public RunTimeCananicalInfo(int validNum, int totalNum, int totalLen) {
			this.validNum = validNum;
			this.totalNum = totalNum;
			this.totalLen = totalLen;
		}

		public void add(RunTimeCananicalInfo info) {
			totalNum += info.totalNum;
			validNum += info.validNum;
			totalLen += info.totalLen;
		}

		public void addTotalLen(int totalLen) {
			this.totalLen += totalLen;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("total valid methods: ").append(validNum).append("\n").append("total methods: ").append(totalNum)
					.append("\n").append("total LOC: ").append(totalLen);
			return sb.toString();
		}
	}
}
