/**
 *  Copyright 2009 Welocalize, Inc. 
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */

/*===========================================================================
 Additional changes Copyright (C) 2009 by the Okapi Framework contributors
 ===========================================================================*/

package net.sf.okapi.steps.gcaligner;

/**
 * DpFunction is an interface defining a method to calculate the score of a matrix cell. The implementation of this
 * interface should have the knowledge of the type of the sequences to be aligned.
 */

public interface DpFunction<T> {
	/**
	 * Set the score to DpMatrixCell at the specified location of the matrix.
	 * 
	 * @param xPos
	 *            X index of the matrix.
	 * @param yPos
	 *            Y index of the matrix.
	 * @param matrix
	 *            matrix
	 */
	void setCellScore(int xPos, int yPos, DpMatrix<T> matrix);
}
