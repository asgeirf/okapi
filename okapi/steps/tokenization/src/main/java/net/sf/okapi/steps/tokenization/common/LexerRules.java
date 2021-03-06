/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  This library is free software; you can redistribute it and/or modify it 
  under the terms of the GNU Lesser General Public License as published by 
  the Free Software Foundation; either version 2.1 of the License, or (at 
  your option) any later version.

  This library is distributed in the hope that it will be useful, but 
  WITHOUT ANY WARRANTY; without even the implied warranty of 
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
  General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License 
  along with this library; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

  See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html
===========================================================================*/

package net.sf.okapi.steps.tokenization.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;

import net.sf.okapi.common.ParametersString;
import net.sf.okapi.lib.extra.AbstractParameters;

/**
 * Lexer rules
 * 
 * @version 0.1 06.07.2009
 */

public class LexerRules extends AbstractParameters implements List<LexerRule> {
	
	private List<LexerRule> rules = new ArrayList<LexerRule>();
	
	private TreeMap<Integer, LexerRule> idMap = new TreeMap<Integer, LexerRule>();
	
	protected Class<? extends LexerRule> getRuleClass() {
		
		return LexerRule.class;
	}
	
	@Override
	protected void parameters_init() {
		
	}

	@Override
	public void parameters_reset() {

		if (rules != null)
			rules.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void parameters_load(ParametersString buffer) {
		
		loadGroup(buffer, "Rule", rules, (Class<LexerRule>) getRuleClass());
		
		// Fix lexemId to be unique		
		List<Integer> usedIDs = new ArrayList<Integer>();
		
		for (LexerRule rule : rules) {
			
			int id = rule.getLexemId();
			
			if (id == 0 && usedIDs.contains(id)) { // First 0 is allowed
				
				while (usedIDs.contains(id))
					id++;
				
				rule.setLexemId(id);
			}
			
			usedIDs.add(id);
		}
		
		idMap.clear();
		
		for (LexerRule rule : rules)			
			idMap.put(rule.getLexemId(), rule);
	}
	
	@Override
	public void parameters_save(ParametersString buffer) {
		
		saveGroup(buffer, "Rule", rules);
	}	
	
	/**
	 * Gets a lexer rule for the given lexem ID.
	 * @param lexemId ID of the rule.
	 * @return LexerRule object of null if no rule has been assigned to the given lexem ID.  
	 */
	public LexerRule getRule(int lexemId) {
		
		return idMap.get(lexemId);
	}

	
//	public List<LexerRule> getItems() {
//		
//		return items;
//	}	

	// List implementation 
	public boolean add(LexerRule o) {
		
		return rules.add(o);
	}

	public void add(int index, LexerRule element) {
		
		rules.add(index, element);
	}

	public boolean addAll(Collection<? extends LexerRule> c) {

		return rules.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends LexerRule> c) {
		
		return rules.addAll(index, c);
	}

	public void clear() {
		
		rules.clear();
	}

	public boolean contains(Object o) {
		
		return rules.contains(o);
	}

	public boolean containsAll(Collection<?> c) {

		return rules.containsAll(c);
	}

	public LexerRule get(int index) {
		
		return rules.get(index);
	}

	public int indexOf(Object o) {

		return rules.indexOf(o);
	}

	public boolean isEmpty() {

		return rules.isEmpty();
	}

	public Iterator<LexerRule> iterator() {
	
		return rules.iterator();
	}

	public int lastIndexOf(Object o) {

		return rules.lastIndexOf(o);
	}

	public ListIterator<LexerRule> listIterator() {

		return rules.listIterator();
	}

	public ListIterator<LexerRule> listIterator(int index) {

		return rules.listIterator(index);
	}

	public boolean remove(Object o) {
	
		return rules.remove(o);
	}

	public LexerRule remove(int index) {

		return rules.remove(index);
	}

	public boolean removeAll(Collection<?> c) {

		return rules.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {

		return rules.retainAll(c);
	}

	public LexerRule set(int index, LexerRule element) {

		return rules.set(index, element);
	}

	public int size() {

		return rules.size();
	}

	public List<LexerRule> subList(int fromIndex, int toIndex) {

		return rules.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {

		return rules.toArray();
	}

	public <T> T[] toArray(T[] a) {

		return rules.toArray(a);
	}

	public boolean hasOutTokens() {
		
		for (LexerRule rule : this) {
			
			if (rule == null) continue;
			if (rule.getOutTokenIDs() == null) continue;
			
			if (rule.getOutTokenIDs().size() > 0)
				return true;
		}
		return false;
	}
}
