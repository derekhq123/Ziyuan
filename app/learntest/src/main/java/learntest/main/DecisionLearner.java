package learntest.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import icsetlv.common.dto.BreakpointValue;
import learntest.breakpoint.data.DecisionLocation;
import learntest.calculator.OrCategoryCalculator;
import learntest.cfg.traveller.CfgConditionManager;
import learntest.sampling.JacopSelectiveSampling;
import learntest.sampling.jacop.StoreSearcher;
import learntest.svm.MyPositiveSeparationMachine;
import learntest.testcase.data.BreakpointData;
import learntest.testcase.data.LoopTimesData;
import libsvm.svm_model;
import libsvm.core.Category;
import libsvm.core.CategoryCalculator;
import libsvm.core.Divider;
import libsvm.core.FormulaProcessor;
import libsvm.core.Machine;
import libsvm.core.Machine.DataPoint;
import libsvm.core.Model;
import sav.common.core.Pair;
import sav.common.core.SavException;
import sav.common.core.formula.AndFormula;
import sav.common.core.formula.Formula;
import sav.common.core.utils.CollectionUtils;
import sav.settings.SAVExecutionTimeOutException;
import sav.settings.SAVTimer;
import sav.strategies.dto.execute.value.ExecValue;
import sav.strategies.dto.execute.value.ExecVar;
import sav.strategies.dto.execute.value.ExecVarType;

public class DecisionLearner implements CategoryCalculator {
	
	protected static Logger log = LoggerFactory.getLogger(DecisionLearner.class);
	private MyPositiveSeparationMachine machine;
	// one class does not perform well
	//private Machine oneClass;
	private List<ExecVar> originVars;
	private List<ExecVar> vars;
	private List<String> labels;
	//private Set<ExecVar> boolVars;
	private JacopSelectiveSampling selectiveSampling;
	
	private CfgConditionManager manager;
	private List<Divider> curDividers;
	
	private Map<DecisionLocation, BreakpointData> bkpDataMap;
	
	private List<BreakpointValue> records;
	
	//private final int MAX_ATTEMPT = 10;
	//private final int MAX_TO_RECORD = 5;
	
	//private long startTime;
	
	private boolean random = true;
	
	private double curBranch = 1;
	
	private boolean needFalse = true;
	private boolean needTrue = true;
	private boolean needOne = true;
	private boolean needMore = true;
	
	public DecisionLearner(JacopSelectiveSampling selectiveSampling, CfgConditionManager manager, boolean random) {
		machine = new MyPositiveSeparationMachine();
		machine.setDefaultParams();
		/*oneClass = new Machine();
		oneClass.setParameter(new Parameter().setMachineType(MachineType.ONE_CLASS)
				.setKernelType(KernelType.LINEAR).setEps(0.00001).setUseShrinking(false)
				.setPredictProbability(false).setC(Double.MAX_VALUE));*/
		this.selectiveSampling = selectiveSampling;
		this.manager = manager;
		this.random = random;
	}
	
	public void learn(Map<DecisionLocation, BreakpointData> bkpDataMap) throws SavException, SAVExecutionTimeOutException {
		manager.updateRelevance(bkpDataMap);
		records = new ArrayList<BreakpointValue>();
		this.bkpDataMap = bkpDataMap;
		List<BreakpointData> bkpDatas = new ArrayList<BreakpointData>(bkpDataMap.values());
		Collections.sort(bkpDatas);
		Map<DecisionLocation, Pair<Formula, Formula>> decisions = new HashMap<DecisionLocation, Pair<Formula, Formula>>();
		for (BreakpointData bkpData : bkpDatas) {
			log.info("Start to learn at " + bkpData.getLocation());
			if (bkpData.getFalseValues().isEmpty() && bkpData.getTrueValues().isEmpty()) {
				log.info("Missing data");
				continue;
			}
			if (vars == null && !collectAllVars(bkpData)) {
				log.info("Missing variables");
				continue;
			}
			Pair<Formula, Formula> res = learn(bkpData);
			manager.setCondition(bkpData.getLocation().getLineNo(), res, curDividers);
			decisions.put(bkpData.getLocation(), res);
		}
		if (!random) {
			Set<Entry<DecisionLocation, Pair<Formula, Formula>>> entrySet = decisions.entrySet();
			try {
				FileWriter writer = new FileWriter(new File("trees/" + LearnTestConfig.getSimpleClassName() + "." 
						+ LearnTestConfig.testMethodName));
				for (Entry<DecisionLocation, Pair<Formula, Formula>> entry : entrySet) {
					writer.write(entry.getKey() + "\n");
					Pair<Formula, Formula> formulas = entry.getValue();
					writer.write("True/False Decision: " + formulas.first() + "\n");
					if (entry.getKey().isLoop()) {
						writer.write("One/More Decision: " + formulas.second() + "\n");
					}
				}
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
		/*for (Entry<DecisionLocation, Pair<Formula, Formula>> entry : entrySet) {
			System.out.println(entry.getKey());
			Pair<Formula, Formula> formulas = entry.getValue();
			System.out.println("True/False Decision: " + formulas.first());
			if (formulas.second() != null) {
				System.out.println("One/More Decision: " + formulas.second());
			}
		}*/
	}
	
	/*private void clear(BreakpointData breakpointData) {
		Iterator<BreakpointValue> falseValues = breakpointData.getFalseValues().iterator();
		while (falseValues.hasNext()) {
			BreakpointValue breakpointValue = (BreakpointValue) falseValues.next();
			int x = breakpointValue.getValue("x", 0.0).intValue();
			int y = breakpointValue.getValue("y", 0.0).intValue();
			int z = breakpointValue.getValue("z", 0.0).intValue();
			if (x >= y && y >= z) {
				continue;
			}
			falseValues.remove();
		}
		Iterator<BreakpointValue> trueValues = breakpointData.getTrueValues().iterator();
		while (trueValues.hasNext()) {
			BreakpointValue breakpointValue = (BreakpointValue) trueValues.next();
			int x = breakpointValue.getValue("x", 0.0).intValue();
			int y = breakpointValue.getValue("y", 0.0).intValue();
			int z = breakpointValue.getValue("z", 0.0).intValue();
			if (x >= y && y >= z) {
				continue;
			}
			trueValues.remove();
		}
	}*/
	
	private Pair<Formula, Formula> learn(BreakpointData bkpData) throws SavException, SAVExecutionTimeOutException {
		needFalse = true;
		needTrue = !(bkpData instanceof LoopTimesData);
		
		OrCategoryCalculator preconditions = null;
		if (!random) {
			preconditions = manager.getPreConditions(bkpData.getLocation().getLineNo());
			preconditions.clear(bkpData);
		}
		
		//updateCoverage(bkpData);

		//clear(bkpData);
		/*if (random) {
			while (bkpData.getTrueValues().isEmpty() || bkpData.getFalseValues().isEmpty()) {
				if(SAVTimer.isTimeOut()){
					throw new SAVExecutionTimeOutException("Time out in random learn");
				}
				Map<DecisionLocation, BreakpointData> selectMap = selectiveSampling.randomSelectData(originVars);
				if (selectMap != null) {
					mergeMap(selectMap);
				}
			}
		}*/
		
		if (bkpData.getTrueValues().isEmpty() || bkpData.getFalseValues().isEmpty()) {			
			//startTime = System.currentTimeMillis();
			Map<DecisionLocation, BreakpointData> selectMap = selectiveSampling.selectDataForEmpty(bkpData.getLocation(), originVars,
					preconditions, null, bkpData.getTrueValues().isEmpty(), false);
			//System.out.println("learn select data for empty time: " + (System.currentTimeMillis() - startTime) + "ms");
			if (selectMap != null) {
				mergeMap(selectMap);
				manager.updateRelevance(bkpDataMap);
				//updateCoverage(bkpData);
				if (bkpData.getTrueValues().isEmpty() || bkpData.getFalseValues().isEmpty()) {
					selectMap = selectiveSampling.selectDataForEmpty(bkpData.getLocation(), originVars, 
							preconditions, null, bkpData.getTrueValues().isEmpty(), false);
					if (selectMap != null) {
						mergeMap(selectMap);
					} else {
						mergeMap(selectiveSampling.getSelectResult());
					}
					manager.updateRelevance(bkpDataMap);
					//updateCoverage(bkpData);
				}
				//bkpData = bkpDataMap.get(bkpData.getLocation());
			} else {
				mergeMap(selectiveSampling.getSelectResult());
				manager.updateRelevance(bkpDataMap);
			}
		}
		
		updateCoverage(bkpData);
				
		if (bkpData.getTrueValues().isEmpty()) {
			log.info("Missing true branch data");
			curDividers = null;
			return new Pair<Formula, Formula>(null, null);
		} else if (bkpData.getFalseValues().isEmpty()) {
			log.info("Missing false branch data");
			curDividers = null;
			Formula oneMore = null;
			if (bkpData instanceof LoopTimesData) {
				oneMore = learn((LoopTimesData)bkpData);
			}
			return new Pair<Formula, Formula>(null, oneMore);
		}
		
		if (random) {
			List<BreakpointValue> falseValues = bkpData.getFalseValues();
			for (BreakpointValue value : falseValues) {
				if (!records.contains(value)) {
					records.add(value);
				}
			}
			
			if (bkpData instanceof LoopTimesData) {
				learn((LoopTimesData)bkpData);
			} else {
				List<BreakpointValue> trueValues = bkpData.getTrueValues();
				for (BreakpointValue value : trueValues) {
					if (!records.contains(value)) {
						records.add(value);
					}
				}
			}
			
			return new Pair<Formula, Formula>(null, null);
		}
		
		/*oneClass.resetData();
		BreakpointData oneClassData = bkpData;
		int times = 0;
		boolean missTrue = oneClassData.getTrueValues().isEmpty();
		boolean missFalse = oneClassData.getFalseValues().isEmpty();
		while ((missFalse || missTrue) && times < MAX_ATTEMPT) {
			addDataPoints(vars, oneClassData.getTrueValues(), Category.POSITIVE, oneClass);
			addDataPoints(vars, oneClassData.getFalseValues(), Category.NEGATIVE, oneClass);
			oneClass.train();
			Formula boundary = oneClass.getLearnedLogic(new FormulaProcessor<ExecVar>(vars), true);
			BreakpointData newData = selectiveSampling.selectData(oneClassData.getLocation(), 
					boundary, oneClass.getDataLabels(), oneClass.getDataPoints());
			if (newData == null) {
				break;
			}
			missTrue &= newData.getTrueValues().isEmpty();
			missFalse &= newData.getFalseValues().isEmpty();
			oneClassData = newData;
			times ++;
		}
		if (missTrue) {
			log.info("Missing true branch data");
			return new Pair<Formula, Formula>(null, null);
		}
		if (missFalse) {
			log.info("Missing false branch data");
			return new Pair<Formula, Formula>(null, null);
		}
		if (bkpData.getTrueValues().isEmpty() || bkpData.getFalseValues().isEmpty()) {
			bkpData.merge(oneClassData);
		}*/
		
		Formula trueFlase = null;
		//manager.updateRelevance(bkpData);
		
		if (/*!manager.isEnd(bkpData.getLocation().getLineNo())*/manager.isRelevant(bkpData.getLocation().getLineNo())) {
			
			int times = 0;
			machine.resetData();
			addDataPoints(originVars, bkpData.getTrueValues(), Category.POSITIVE, machine);
			addDataPoints(originVars, bkpData.getFalseValues(), Category.NEGATIVE, machine);
			//startTime = System.currentTimeMillis();
			machine.train();
			//System.out.println("learn model training time: " + (System.currentTimeMillis() - startTime) + " ms");
			/*Formula */trueFlase = getLearnedFormula();
			double acc = machine.getModelAccuracy();
			curDividers = machine.getLearnedDividers();
			while(trueFlase != null /*&& times < MAX_ATTEMPT*/ && manager.isRelevant(bkpData.getLocation().getLineNo())) {
				/*BreakpointData newData = selectiveSampling.selectData(bkpData.getLocation(), 
						trueFlase, machine.getDataLabels(), machine.getDataPoints());*/
				Map<DecisionLocation, BreakpointData> newMap = selectiveSampling.selectDataForModel(bkpData.getLocation(), 
						originVars, machine.getDataPoints(), preconditions, machine.getLearnedDividers());
				if (newMap == null) {
					break;
				}
				mergeMap(newMap);
				manager.updateRelevance(bkpDataMap);
				
				//preconditions.clear(newData);
				BreakpointData newData = newMap.get(bkpData.getLocation());
				if (newData == null) {
					break;
				}
				preconditions.clear(newData);
				//manager.updateRelevance(bkpData);
				addDataPoints(originVars, newData.getTrueValues(), Category.POSITIVE, machine);
				addDataPoints(originVars, newData.getFalseValues(), Category.NEGATIVE, machine);
				/*if (machine.getModelAccuracy() == 1.0) {
					break;
				}*/
				//startTime = System.currentTimeMillis();
				machine.train();
				//System.out.println("learn model training time: " + (System.currentTimeMillis() - startTime) + " ms");
				Formula tmp = getLearnedFormula();
				double accTmp = machine.getModelAccuracy();
				if (tmp == null) {
					trueFlase = null;
					curDividers = machine.getLearnedDividers();
					break;
				}
				if (!tmp.equals(trueFlase) && accTmp > acc) {
					trueFlase = tmp;
					curDividers = machine.getLearnedDividers();
					acc = accTmp;
				} else {
					break;
				}
				times ++;
			}
		
		}
		
		/*List<BreakpointValue> falseValues = bkpData.getFalseValues();
		boolean needMore = true;
		for (BreakpointValue value : falseValues) {
			if (records.contains(value)) {
				needMore = false;
				break;
			}
		}
		if (needMore) {
			records.add(falseValues.get(0));
		}*/
		
		Formula oneMore = null;
		if (bkpData instanceof LoopTimesData) {
			oneMore = learn((LoopTimesData)bkpData);
		} /*else {
			List<BreakpointValue> trueValues = bkpData.getTrueValues();
			needMore = true;
			for (BreakpointValue value : trueValues) {
				if (records.contains(value)) {
					needMore = false;
					break;
				}
			}
			if (needMore) {
				records.add(trueValues.get(0));
			}
		}*/
		
		/*if (!bkpData.getFalseValues().isEmpty() && bkpData.getFalseValues().size() < MAX_TO_RECORD) {
			records.add(bkpData.getFalseValues().get(0));
		}
		if (!(bkpData instanceof LoopTimesData) && !bkpData.getTrueValues().isEmpty() && bkpData.getTrueValues().size() < MAX_TO_RECORD) {
			records.add(bkpData.getTrueValues().get(0));
		}*/
		return new Pair<Formula, Formula>(trueFlase, oneMore);
	}
	
	private Formula learn(LoopTimesData loopData) throws SavException, SAVExecutionTimeOutException {
		needOne = true;
		needMore = true;
		
		OrCategoryCalculator preConditions = null;
		if (!random) {
			preConditions = manager.getPreConditions(loopData.getLocation().getLineNo());			
		}
		/*if (random) {
			while (loopData.getOneTimeValues().isEmpty() || loopData.getMoreTimesValues().isEmpty()) {
				if(SAVTimer.isTimeOut()){
					throw new SAVExecutionTimeOutException("Time out in random learn");
				}
				Map<DecisionLocation, BreakpointData> selectMap = selectiveSampling.randomSelectData(originVars);
				if (selectMap != null) {
					mergeMap(selectMap);
				}
			}
		}*/
		//updateCoverage(loopData);
		if (loopData.getOneTimeValues().isEmpty() || loopData.getMoreTimesValues().isEmpty()) {
			//startTime = System.currentTimeMillis();
			Map<DecisionLocation, BreakpointData> selectMap = selectiveSampling.selectDataForEmpty(loopData.getLocation(), originVars,
					preConditions, curDividers, loopData.getMoreTimesValues().isEmpty(), true);
			//System.out.println("learn select data for empty time: " + (System.currentTimeMillis() - startTime) + "ms");
			if (selectMap != null) {
				mergeMap(selectMap);			
			} else {
				mergeMap(selectiveSampling.getSelectResult());
			}
			manager.updateRelevance(bkpDataMap);
			//updateCoverage(loopData);	
			//loopData = (LoopTimesData) bkpDataMap.get(loopData.getLocation());
		}
		updateCoverage(loopData);
		if (loopData.getOneTimeValues().isEmpty()) {
			log.info("Missing once loop data");
			return null;
		} else if (loopData.getMoreTimesValues().isEmpty()) {
			log.info("Missing more than once loop data");
			return null;
		}
		
		if (random) {
			List<BreakpointValue> choices = loopData.getOneTimeValues();
			for (BreakpointValue value : choices) {
				if (!records.contains(value)) {
					records.add(value);
				}
			}
			choices = loopData.getMoreTimesValues();
			for (BreakpointValue value : choices) {
				if (!records.contains(value)) {
					records.add(value);
				}
			}
			return null;
		}
		
		/*oneClass.resetData();
		int times = 0;
		LoopTimesData oneClassData = loopData;
		boolean missOnce = oneClassData.getOneTimeValues().isEmpty();
		boolean missMore = oneClassData.getMoreTimesValues().isEmpty();
		while ((missOnce || missMore) && times < MAX_ATTEMPT) {
			addDataPoints(vars, oneClassData.getMoreTimesValues(), Category.POSITIVE, oneClass);
			addDataPoints(vars, oneClassData.getOneTimeValues(), Category.NEGATIVE, oneClass);
			oneClass.train();
			Formula boundary = oneClass.getLearnedLogic(new FormulaProcessor<ExecVar>(vars), true);
			LoopTimesData newData = (LoopTimesData) selectiveSampling.selectData(oneClassData.getLocation(), 
					boundary, oneClass.getDataLabels(), oneClass.getDataPoints());
			if (newData == null) {
				break;
			}
			missOnce &= newData.getOneTimeValues().isEmpty();
			missMore &= newData.getMoreTimesValues().isEmpty();
			oneClassData = newData;
			times ++;
		}
		if (missOnce) {
			log.info("Missing once loop data");
			return null;
		} 
		if (missMore) {
			log.info("Missing more than once loop data");
			return null;
		}
		if (loopData.getOneTimeValues().isEmpty() || loopData.getMoreTimesValues().isEmpty()) {
			loopData.merge(oneClassData);
		}*/
		Formula formula = null;
		//manager.updateRelevance(loopData);
		if (/*!manager.isEnd(loopData.getLocation().getLineNo())*/manager.isRelevant(loopData.getLocation().getLineNo())) {
			
			int times = 0;
			machine.resetData();
			addDataPoints(originVars, loopData.getMoreTimesValues(), Category.POSITIVE, machine);
			addDataPoints(originVars, loopData.getOneTimeValues(), Category.NEGATIVE, machine);
			//startTime = System.currentTimeMillis();
			machine.train();
			//System.out.println("learn model training time: " + (System.currentTimeMillis() - startTime) + " ms");
			formula = getLearnedFormula();
			double acc = machine.getModelAccuracy();
			while(formula != null && /*times < MAX_ATTEMPT &&*/ manager.isRelevant(loopData.getLocation().getLineNo())) {
				/*LoopTimesData newData = (LoopTimesData) selectiveSampling.selectData(loopData.getLocation(), 
						formula, machine.getDataLabels(), machine.getDataPoints());	*/
				Map<DecisionLocation, BreakpointData> newMap = selectiveSampling.selectDataForModel(loopData.getLocation(), 
						originVars, machine.getDataPoints(), preConditions, machine.getLearnedDividers());
				if (newMap == null) {
					break;
				}
				mergeMap(newMap);
				manager.updateRelevance(bkpDataMap);
				LoopTimesData newData = (LoopTimesData) newMap.get(loopData.getLocation());
				if (newData == null) {
					break;
				}
				//manager.updateRelevance(loopData);
				addDataPoints(originVars, newData.getMoreTimesValues(), Category.POSITIVE, machine);
				addDataPoints(originVars, newData.getOneTimeValues(), Category.NEGATIVE, machine);
				/*if (machine.getModelAccuracy() == 1.0) {
					break;
				}*/
				//startTime = System.currentTimeMillis();
				machine.train();
				//System.out.println("learn model training time: " + (System.currentTimeMillis() - startTime) + " ms");
				Formula tmp = getLearnedFormula();
				double accTmp = machine.getModelAccuracy();
				if (tmp == null) {
					break;
				}
				if (!tmp.equals(formula) && accTmp > acc) {
					formula = tmp;
					acc = accTmp;
				} else {
					break;
				}
				times ++;
			}
			
		}	
		
		/*List<BreakpointValue> choices = loopData.getOneTimeValues();
		boolean needMore = true;
		for (BreakpointValue value : choices) {
			if (records.contains(value)) {
				needMore = false;
				break;
			}
		}
		if (needMore) {
			records.add(choices.get(0));
		}
		choices = loopData.getMoreTimesValues();
		needMore = true;
		for (BreakpointValue value : choices) {
			if (records.contains(value)) {
				needMore = false;
				break;
			}
		}
		if (needMore) {
			records.add(choices.get(0));
		}*/
		
		/*if (!loopData.getOneTimeValues().isEmpty() && loopData.getOneTimeValues().size() < MAX_TO_RECORD) {
			records.add(loopData.getOneTimeValues().get(0));
		}
		if (!loopData.getMoreTimesValues().isEmpty() && loopData.getMoreTimesValues().size() < MAX_TO_RECORD) {
			records.add(loopData.getMoreTimesValues().get(0));
		}*/
		
		return formula;
	}
	
	private boolean collectAllVars(BreakpointData bkpData) {
		Set<ExecVar> allVars = new HashSet<ExecVar>();
		for (ExecValue bkpVal : bkpData.getFalseValues()) {
			collectExecVar(bkpVal.getChildren(), allVars);
		}
		for (ExecValue bkpVal : bkpData.getTrueValues()) {
			collectExecVar(bkpVal.getChildren(), allVars);
		}
		originVars = new ArrayList<ExecVar>(allVars);
		StoreSearcher.length = originVars.size();
		//calculateNums();
		if (originVars.isEmpty()) {
			return false;
		}
		mappingVars();
		//boolVars = extractBoolVars(vars);
		//vars.removeAll(boolVars);
		labels = extractLabels(vars);
		machine.setDataLabels(labels);
		//oneClass.setDataLabels(labels);
		manager.setVars(vars, originVars);
		return true;
	}

	private void collectExecVar(List<ExecValue> vals, Set<ExecVar> vars) {
		if (CollectionUtils.isEmpty(vals)) {
			return;
		}
		for (ExecValue val : vals) {
			if (val == null || CollectionUtils.isEmpty(val.getChildren())) {
				String varId = val.getVarId();
				vars.add(new ExecVar(varId, val.getType()));
			}
			collectExecVar(val.getChildren(), vars);
		}
	}

	/*private void calculateNums() {
		int num = 1;
		for (ExecVar var : originVars) {
			switch (var.getType()) {
				case BOOLEAN:
					num *= 2;
					break;
				case INTEGER:
					num *= 400;
					break;
				case BYTE:
					num *= 200;
					break;
				case CHAR:
					num *= 200;
					break;
				case DOUBLE:
					num *= 4000;
					break;
				case FLOAT:
					num *= 2000;
					break;
				case LONG:
					num *= 2000;
					break;
				case SHORT:
					num *= 200;
					break;
				default:
					break;
			}
		}
		selectiveSampling.setNumLimit(num / 10);
	}*/
	
	/*private Set<ExecVar> extractBoolVars(List<ExecVar> allVars) {
		Set<ExecVar> result = new HashSet<ExecVar>();
		for (ExecVar var : allVars) {
			if (var.getType() == ExecVarType.BOOLEAN) {
				result.add(var);
			}
		}
		return result;
	}*/
	
	private void mappingVars() {
		vars = new ArrayList<ExecVar>(originVars);
		int size = originVars.size();
		for (int i = 0; i < size; i++) {
			ExecVar var = originVars.get(i);
			for (int j = i; j < size; j++) {
				vars.add(new ExecVar(var.getLabel() + " * " + originVars.get(j).getLabel(), 
						ExecVarType.INTEGER));
			}
		}
	}
	
	private List<String> extractLabels(List<ExecVar> allVars) {
		List<String> labels = new ArrayList<String>(allVars.size());
		for (ExecVar var : allVars) {
			labels.add(var.getVarId());
		}
		return labels;
	}
	
	private void updateCoverage(BreakpointData data) {
		if (needFalse) {
			List<BreakpointValue> falseValues = data.getFalseValues();
			for (BreakpointValue value : falseValues) {
				if (records.contains(value)) {
					needFalse = false;
					curBranch += 1;
					break;
				}
			}
			if (needFalse && !falseValues.isEmpty()) {
				records.add(falseValues.get(0));
				needFalse = false;
				curBranch += 1;
			}
		}
		if (needTrue) {
			List<BreakpointValue> trueValues = data.getTrueValues();
			for (BreakpointValue value : trueValues) {
				if (records.contains(value)) {
					needTrue = false;
					curBranch += 1;
					break;
				}
			}
			if (needTrue && !trueValues.isEmpty()) {
				records.add(trueValues.get(0));
				needTrue = false;
				curBranch += 1;
			}
		}
	}
	
	private void updateCoverage(LoopTimesData data) {
		if (needOne) {
			List<BreakpointValue> oneTimeValues = data.getOneTimeValues();
			for (BreakpointValue value : oneTimeValues) {
				if (records.contains(value)) {
					needOne = false;
					curBranch += 1;
					break;
				}
			}
			if (needOne && !oneTimeValues.isEmpty()) {
				records.add(oneTimeValues.get(0));
				needOne = false;
				curBranch += 1;
			}
		}
		if (needMore) {
			List<BreakpointValue> moreTimesValues = data.getMoreTimesValues();
			for (BreakpointValue value : moreTimesValues) {
				if (records.contains(value)) {
					needMore = false;
					curBranch += 1;
					break;
				}
			}
			if (needMore && !moreTimesValues.isEmpty()) {
				records.add(moreTimesValues.get(0));
				needMore = false;
				curBranch += 1;
			}
		}
	}
	
	private void addDataPoints(List<ExecVar> vars, List<BreakpointValue> values, Category category, Machine machine) {
		for (BreakpointValue value : values) {
			addDataPoint(vars, value, category, machine);
		}
	}
	
	private void addDataPoint(List<ExecVar> vars, BreakpointValue bValue, Category category, Machine machine) {
		double[] lineVals = new double[labels.size()];
		int i = 0;
		for (ExecVar var : vars) {
			final Double value = bValue.getValue(var.getLabel(), 0.0);
			lineVals[i++] = value;
		}
		int size = vars.size();
		for (int j = 0; j < size; j++) {
			double value = bValue.getValue(vars.get(j).getLabel(), 0.0);
			for (int k = j; k < size; k++) {
				//lineVals[i ++] = value * bValue.getValue(vars.get(k).getLabel(), 0.0);
				lineVals[i ++] = 0.0;
			}
		}

		machine.addDataPoint(category, lineVals);
	}

	private Formula getLearnedFormula() {
		Formula formula = null;
		List<svm_model> models = machine.getLearnedModels();
		final int numberOfFeatures = machine.getNumberOfFeatures();
		if (models != null && numberOfFeatures > 0) {			
			for (svm_model svmModel : models) {
				if (svmModel != null) {				
					final Divider explicitDivider = new Model(svmModel, numberOfFeatures).getExplicitDivider();
					Formula current = new FormulaProcessor<ExecVar>(vars).process(explicitDivider, machine.getDataLabels(), true);
					if (formula == null) {
						formula = current;
					} else {
						formula = new AndFormula(formula, current);
					}
				}
			}
		}
		return formula;
	}
	
	@Override
	public Category getCategory(DataPoint dataPoint) {
		return null;
	}

	public List<String> getLabels() {
		return labels;
	}

	public List<ExecVar> getOriginVars() {
		return originVars;
	}
	
	private void mergeMap(Map<DecisionLocation, BreakpointData> newMap) {
		//startTime = System.currentTimeMillis();
		if (newMap == null) {
			return;
		}
		Set<DecisionLocation> locations = bkpDataMap.keySet();
		for (DecisionLocation location : locations) {
			bkpDataMap.get(location).merge(newMap.get(location));
		}
		//System.out.println("learn merge map time: " + (System.currentTimeMillis() - startTime) + " ms");
		return;
	}

	public List<BreakpointValue> getRecords() {
		return records;
	}

	public double getCoverage() {
		return curBranch / manager.getTotalBranch();
	}

}
