/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.engine.algorithm.iface;

import lstar.LStarException;
import tzuyu.engine.iface.HasReport;
import tzuyu.engine.model.dfa.Alphabet;
import tzuyu.engine.model.dfa.DFA;

/**
 * @author LLT
 * The learner should not know anything about the alphabet instant.
 */
public interface Learner<A extends Alphabet> extends HasReport<A> {
	/**
	 * main function of the algorithm
	 */
	public DFA startLearning(A sig) throws LStarException, InterruptedException;
	
	public void setTeacher(Teacher<A> teacher);
}
