/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package jdart.core;

/**
 * @author LLT
 *
 */
public class JDartParams {
	private String appProperties;
	private String siteProperties;
	private String mainEntry;
	private String className;
	private String classpathStr;
	private String methodName;
	private String methodParamsStr;
	private long timeLimit; // run time limit, unit of ms
	private long minFree; // min free memory, unit of byte
	private int explore_node = -1;
	private int explore_branch = -1;
	private int limitNumberOfResultSet = 500; // default, copy from JDartClient class.
	
	public JDartParams() {
		
	}

	public String getMainEntry() {
		return mainEntry;
	}

	public void setMainEntry(String mainEntry) {
		this.mainEntry = mainEntry;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getClasspathStr() {
		return classpathStr;
	}

	public void setClasspathStr(String classpathStr) {
		this.classpathStr = classpathStr;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getParamString() {
		return methodParamsStr;
	}

	public void setParamString(String paramString) {
		this.methodParamsStr = paramString;
	}

	public String getAppProperties() {
		return appProperties;
	}

	public void setAppProperties(String appProperties) {
		this.appProperties = appProperties;
	}

	public String getSiteProperties() {
		return siteProperties;
	}

	public void setSiteProperties(String siteProperties) {
		this.siteProperties = siteProperties;
	}

	public long getTimeLimit() {
		return timeLimit;
	}

	public void setTimeLimit(long timeLimit) {
		this.timeLimit = timeLimit;
	}

	public long getMinFree() {
		return minFree;
	}

	public void setMinFree(long minFree) {
		this.minFree = minFree;
	}

	public int getExploreNode() {
		return explore_node;
	}

	public void setExploreNode(int explore_node) {
		this.explore_node = explore_node;
	}

	public int getExploreBranch() {
		return explore_branch;
	}

	public void setExploreBranch(int explore_branch) {
		this.explore_branch = explore_branch;
	}
	
	public int getLimitNumberOfResultSet() {
		return limitNumberOfResultSet;
	}

	public void setLimitNumberOfResultSet(int limitNumberOfResultSet) {
		this.limitNumberOfResultSet = limitNumberOfResultSet;
	}

	
	public static JDartParams constructOnDemandJDartParams(String classpathStr, String mainEntry, String className, 
			String methodName, String paramString,
			String app, String site, int node, int branch) {
		
		long minFree = (1024<<10); // min free memory
		long timeLimit = 60 * 1000; // ms
		
		JDartParams params = new JDartParams();
		params.setAppProperties(app);
		params.setClassName(className);
		params.setClasspathStr(classpathStr);
		params.setMainEntry(mainEntry);
		params.setMethodName(methodName);
		params.setMinFree(minFree);
		params.setParamString(paramString);
		params.setSiteProperties(site);
		params.setTimeLimit(timeLimit);
		params.setExploreNode(node);
		params.setExploreBranch(branch);

		return params;
	}
	
	public static JDartParams defaultOnDemandJDartParams() {
		String  app = "libs/jdart/jpf.properties",
				site = "libs/jpf_on_demand.properties",
				
				mainEntry = "testdata.l2t.main.example.foo.Example2",
				className = "com.Example",
				methodName = "foo",
				paramString = "(x:int,y:int)";

		int node = 11; // cfg node index
		int branch = 1; // 0,1 , missing branch
		
		mainEntry = "testdata.L2T.main.fastmath.scalb1.FastMath8";
		className = "org.apache.commons.math.util.FastMath";
		methodName = "scalb1";
		paramString = "(d:double, n:int)";
		node = 41;
		branch = 1;	
		
		mainEntry = "testdata.L2T.main.mathutils.mulandchecki.MathUtils10";
		className = "org.apache.commons.math.util.MathUtils";
		methodName = "mulAndCheckI";
		paramString = "(a:int, b:int)";
		node = 2;
		branch = 1;	
		
//		mainEntry = "testdata.L2T.main.betadistributionimpl.inversecumulativeprobability.BetaDistributionImpl2";
//		className = "org.apache.commons.math.distribution.BetaDistributionImpl";
//		methodName = "inverseCumulativeProbability";
//		paramString = "(p:double)";
//		node = 9;
//		branch = 1;	
		
		mainEntry = "testdata.Jdart.init.xyz.getordinate.XYZMain";
		className = "org.jscience.geography.coordinates.XYZ";
		methodName = "getOrdinate";
		paramString = "(dimension:int)";
		node = 31;
		branch = 1;			

		mainEntry = "testdata.Jdart.init.largeinteger.valueof.LargeIntegerMain";
		className = "org.jscience.mathematics.number.LargeInteger";
		methodName = "valueOf";
		paramString = "(bytes:byte[], offset:int, length:int)";
		node = 103;
		branch = 0;
		
		return constructOnDemandJDartParams(localClasspathStr, mainEntry, className, methodName, paramString, app, site, node, branch);
	}
	
	static String  localClasspathStr ="E:\\git\\test-projects\\jscience\\jscience-master\\target\\classes;D:\\eclipse\\eclipse-java-mars-clean\\eclipse\\plugins\\org.junit_4.12.0.v201504281640\\junit.jar;E:\\git\\test-projects\\jscience\\jscience-master\\lib\\javolution.jar;E:\\git\\test-projects\\jscience\\jscience-master\\lib\\geoapi.jar;E:\\git\\test-projects\\jscience\\jscience-master\\colapi.jar;D:\\eclipse\\eclipse-java-mars-clean\\eclipse\\plugins\\org.hamcrest.core_1.3.0.v201303031735.jar";
	public static JDartParams defaultJDartParams() {
		String 	app = "libs/jdart/jpf.properties",
				site = "libs/jpf.properties", //if only want to solve once, change to libs/jpf_once.properties
				
				mainEntry = "com.Test",
				className = "org.apache.commons.math.util.FastMath",
				methodName = "floor",
				paramString = "(x:double)";
//				mainEntry = "testdata.l2t.init.mersennetwister.next.MersenneTwisterMain",
//				className = "org.apache.commons.math.random.MersenneTwister",
//				methodName = "next",
//				paramString = "(bits:int)";

		/** return one results */
//		mainEntry = "testdata.l2t.init.continuousoutputmodel.setinterpolatedtime.ContinuousOutputModelMain";
//		className = "org.apache.commons.math.ode.ContinuousOutputModel";
//		methodName = "setInterpolatedTime";
//		paramString = "(time:double)";
//		
//		/** return two results */
//		mainEntry = "testdata.l2t.init.mersennetwister.next.MersenneTwisterMain";
//		className = "org.apache.commons.math.random.MersenneTwister";
//		methodName = "next";
//		paramString = "(bits:int)";//line 224
		
		/** classcast exception */
//		mainEntry = "testdata.l2t.init.mersennetwister.setseed.MersenneTwisterMain";
//		className = "org.apache.commons.math.random.MersenneTwister";
//		methodName = "setSeed";
//		paramString = "(seed:int[])";
		mainEntry = "testdata.L2T.main.fastmath.scalb1.FastMath8";
		className = "org.apache.commons.math.util.FastMath";
		methodName = "scalb1";
		paramString = "(d:double, n:int)";
		
		mainEntry = "testdata.L2T.main.mathutils.mulandchecki.MathUtils10";
		className = "org.apache.commons.math.util.MathUtils";
		methodName = "mulAndCheckI";
		paramString = "(a:int, b:int)";		

		mainEntry = "testdata.Jdart.init.xyz.getordinate.XYZMain";
		className = "org.jscience.geography.coordinates.XYZ";
		methodName = "getOrdinate";
		paramString = "(dimension:int)";
		
		mainEntry = "testdata.Jdart.init.largeinteger.valueof.LargeIntegerMain";
		className = "org.jscience.mathematics.number.LargeInteger";
		methodName = "valueOf";
		paramString = "(bytes:byte[], offset:int, length:int)";
		
		return constructJDartParams(localClasspathStr, mainEntry, className, methodName, paramString, app, site);
	}
	
	public static JDartParams constructJDartParams(String classpathStr, String mainEntry, String className, String methodName, String paramString,
			String app, String site) {
		
		long minFree = (1024<<10); // min free memory
		long timeLimit = 60 * 1000; // ms
		
		JDartParams params = new JDartParams();
		params.setAppProperties(app);
		params.setClassName(className);
		params.setClasspathStr(classpathStr);
		params.setMainEntry(mainEntry);
		params.setMethodName(methodName);
		params.setMinFree(minFree);
		params.setParamString(paramString);
		params.setSiteProperties(site);
		params.setTimeLimit(timeLimit);
		
		return params;
	}
}
