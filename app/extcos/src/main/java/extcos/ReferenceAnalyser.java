/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package extcos;

import java.util.Set;

import net.sf.extcos.ComponentQuery;
import net.sf.extcos.ComponentScanner;
import net.sf.extcos.internal.ExtendingTypeFilterImpl;
import net.sf.extcos.internal.ImplementingTypeFilterImpl;
import net.sf.extcos.internal.TypeFilterBasedReturning;
import net.sf.extcos.selector.TypeFilter;
import sav.common.core.utils.Randomness;
import sav.strategies.gentest.IReferencesAnalyzer;

/**
 * @author khanh
 *
 */
public class ReferenceAnalyser implements IReferencesAnalyzer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * sav.strategies.gentest.IReferencesAnalyzer#getRandomImplClzz(java.lang
	 * .Class)
	 */
	@Override
	public Class<?> getRandomImplClzz(Class<?> clazz) {

		if(clazz.isInterface()){
			return getRandomImplInterface(clazz, new ImplementingTypeFilterImpl(clazz));
		}
		else{
			return getRandomImplInterface(clazz, new ExtendingTypeFilterImpl(clazz));
		}
	}

	private Class<?> getRandomImplInterface(final Class<?> clazz, final TypeFilter typeFilter ) {
		ComponentScanner scanner = new ComponentScanner();
		Set<Class<?>> classes = scanner.getClasses(new ComponentQuery() {
			@Override
			protected void query() {
				select().from(clazz.getPackage().getName()).
						returning(
								new TypeFilterBasedReturning(
										typeFilter));
			}
		});
		
		return (Class<?>) Randomness.randomMember(classes.toArray());
		
	}

}
