package com.ibm.ola.agent;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Configuration {

	public static final String DATA_STORE_IMPLEMENTATION_CLASS_KEY = "data-store-class";
	public static final String CLIENT_CLASS_KEY = "client-class";
	
	List<Pattern> inclusionList = new ArrayList<Pattern>();
	List<Pattern> exclusionList = new ArrayList<Pattern>();
	
	Map<Integer, Boolean> includedAndNotExcludedMap = new HashMap<Integer, Boolean>();
	
	Map<String, String> propertyMap = new HashMap<String, String>();
	
	public Configuration() {
		
	}
	
	public String getDataStoreImplementationClass() {
		return getStringValue(DATA_STORE_IMPLEMENTATION_CLASS_KEY);
	}
	
	public String getClientClass() {
		return getStringValue(CLIENT_CLASS_KEY);
	}
	
	public Integer getIntValue(String key) {
		String value = propertyMap.get(key);
		return value == null ? null : Integer.parseInt(value);
	}
	
	public String getStringValue(String key) {
		return propertyMap.get(key);
	}
	
	private boolean isMatchingIncludePattern(String classDotName) {
		if (inclusionList.size() == 0) return true;
		else return isMatched(classDotName, inclusionList);
	}
	
	private boolean isMatchingExcludePattern(String classDotName) {
		return isMatched(classDotName, exclusionList);		
	}
	
	private boolean isMatched(String classDotName, List<Pattern> patternList) {
		Iterator<Pattern> it = patternList.listIterator();
		Pattern pattern;
		while (it.hasNext()) {
			pattern = it.next();
			if (pattern.matcher(classDotName).matches()) 
				return true;
		}
		return false;		
	}
	
	
	public boolean isIncluded(String classDotName) {
		int hashCode = classDotName.hashCode();
		Boolean result = includedAndNotExcludedMap.get(hashCode);
		if (result != null) return result;
		
		boolean isIncluded = isMatchingIncludePattern(classDotName);
		if (!isIncluded) result = false;
		else {
			boolean isExcluded = isMatchingExcludePattern(classDotName);
			result = !isExcluded;
		}
		
		includedAndNotExcludedMap.put(classDotName.hashCode(), result);
		
		return result;
	}
	
	public void addIncludePattern(Pattern pattern) {
		inclusionList.add(pattern);
		propertyMap.put("inclusion=", pattern.pattern());
	}
	
	public void addExcludePattern(Pattern pattern) {
		inclusionList.add(pattern);
		propertyMap.put("exclusion=", pattern.pattern());
	}
	
	public void load(InputStream is) throws IOException {
		propertyMap.clear();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		
		String line;
		String key;
		String value;
		while ( (line = br.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("#")) continue;
			else {
				int index = line.indexOf('=');
				if (index < 0) {
					// log
					System.out.println("Found invalid configuration property: " + line);
				}
				else {
					key = line.substring(0, index);
					value = line.substring(index+1, line.length()).trim();
					
					propertyMap.put(key, value);
					
					if (key.equals("inclusion"))
						inclusionList.add(Pattern.compile(value));
					else if (key.equals("exclusion"))
						exclusionList.add(Pattern.compile(value));
				}
			}
		}
	}
	
	public void store(OutputStream os) throws IOException {
		PrintStream ps = new PrintStream(os);
		Iterator<String> it = propertyMap.keySet().iterator();
		String key;
		while (it.hasNext()) {
			key = it.next();
			ps.println(key + ": " + propertyMap.get(key));
		}
		
		ps.close();
	}
}
