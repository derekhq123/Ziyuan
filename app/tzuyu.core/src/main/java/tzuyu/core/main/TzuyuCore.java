/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.main;

import java.util.List;

import main.FaultLocalization;
import tzuyu.core.inject.ApplicationData;
import tzuyu.core.main.context.AbstractApplicationContext;
import faultLocalization.FaultLocalizationReport;


/**
 * @author LLT
 *
 */
public class TzuyuCore {
	private AbstractApplicationContext appContext;
	private ApplicationData appData;
	
	public TzuyuCore(AbstractApplicationContext appContext) {
		this.appContext = appContext;
		this.appData = appContext.getAppData();
	}

	public FaultLocalizationReport faultLocalization(List<String> testingClassNames,
			List<String> junitClassNames) throws Exception {
		return faultLocalization(testingClassNames, junitClassNames, true);
	}
	
	public FaultLocalizationReport faultLocalization(List<String> testingClassNames,
			List<String> junitClassNames, boolean useSlicer) throws Exception {
		FaultLocalization analyzer = new FaultLocalization(appContext);
		analyzer.setUseSlicer(useSlicer);
		return analyzer.analyse(testingClassNames, junitClassNames,
				appData.getSuspiciousCalculAlgo());
	}
	
	public FaultLocalizationReport faultLocalization2(
			List<String> testingClassNames, List<String> testingPackages,
			List<String> junitClassNames, boolean useSlicer) throws Exception {
		FaultLocalization analyzer = new FaultLocalization(appContext);
		analyzer.setUseSlicer(useSlicer);
		return analyzer.analyseSlicingFirst(testingClassNames, testingPackages,
				junitClassNames,
				appData.getSuspiciousCalculAlgo());
	}
}
