/*
 *  Licensed to GIScience Research Group, Heidelberg University (GIScience)
 *
 *   http://www.giscience.uni-hd.de
 *   http://www.heigit.org
 *
 *  under one or more contributor license agreements. See the NOTICE file 
 *  distributed with this work for additional information regarding copyright 
 *  ownership. The GIScience licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in compliance 
 *  with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package heigit.ors.routing;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import heigit.ors.util.RuntimeUtility;

public class RoutingProfilesCollection {
	private HashMap<Integer, RoutingProfile> m_routeProfiles;
	private ArrayList<RoutingProfile> m_uniqueProfiles;

	public RoutingProfilesCollection() {
		m_routeProfiles = new HashMap<Integer, RoutingProfile>();
		m_uniqueProfiles = new ArrayList<RoutingProfile>();
	}

	public void destroy() {
		for (RoutingProfile rp : m_uniqueProfiles) {
			rp.close();
		}
		
		m_routeProfiles.clear();	
	}
	
	public ArrayList<RoutingProfile> getUniqueProfiles()
	{
		return m_uniqueProfiles;
	}
	
	public int size()
	{
		return m_uniqueProfiles.size();
	}
	
	public void clear() {
		m_routeProfiles.clear();
		m_uniqueProfiles.clear();
	}
	
	public boolean add(RoutingProfile rp )
	{
		boolean result = true;
		
		synchronized (m_uniqueProfiles) {
			m_uniqueProfiles.add(rp);

			Integer[] routePrefs = rp.getPreferences();
			if (routePrefs != null) {
				for (int j = 0; j < routePrefs.length; j++) {
					if (!add(routePrefs[j], rp, false))
						result = false;
				}
			}
		}
		
		return result;
	}

	public boolean add(int routePref, RoutingProfile rp, boolean isUnique) {
		boolean res = false;

		synchronized (m_routeProfiles) {
			int key = getRoutePreferenceKey(routePref, rp.isCHEnabled());

			if (!m_routeProfiles.containsKey(key))
			{
				m_routeProfiles.put(key, rp);
				if (isUnique)
					m_uniqueProfiles.add(rp);
				
				res = true;
			}
			
			if (rp.isCHEnabled())
			{
				//  add the same profile but with dynamic weights
				key = getRoutePreferenceKey(routePref, false);

				if (!m_routeProfiles.containsKey(key))
				{
					m_routeProfiles.put(key, rp);
					if (isUnique)
						m_uniqueProfiles.add(rp);
					
					res = true;
				}
			}
		}

		return res;
	}

	public ArrayList<RoutingProfile> getCarProfiles() {
		ArrayList<RoutingProfile> result = new ArrayList<RoutingProfile>();
		for (RoutingProfile rp : m_routeProfiles.values()) {
			if (rp.hasCarPreferences())
				result.add(rp);
		}

		return result;
	}

	public RoutingProfile getRouteProfile(int routePref, boolean chEnabled) throws Exception {
		int routePrefKey = getRoutePreferenceKey(routePref, chEnabled);
		//Fall back to non-CH version if CH routing profile does not exist
		if (!m_routeProfiles.containsKey(routePrefKey)){
			routePrefKey = getRoutePreferenceKey(routePref, false);
			if (!m_routeProfiles.containsKey(routePrefKey))
				return null;
		}
		RoutingProfile rp = m_routeProfiles.get(routePrefKey);
		return rp;

	}

	/**
	 	 * Check if the CH graph of the specified profile has been built.
	 	 *
	 	 * @param routePref				The chosen routing profile
	 	 */

	public boolean isCHProfileAvailable(int routePref) throws Exception {
		int routePrefKey = getRoutePreferenceKey(routePref, true);
		if (!m_routeProfiles.containsKey(routePrefKey)) return false;
		return true;
	}
	
	public RoutingProfile getRouteProfile(int routePref) throws Exception
	{
		RoutingProfile rp = getRouteProfileByKey(getRoutePreferenceKey(routePref, false));
		//if (rp == null)
		//	rp = getRouteProfileByKey(getRoutePreferenceKey(routePref, false));
		
		return rp;
	}

	private RoutingProfile getRouteProfileByKey(int routePrefKey) throws Exception {
		if (!m_routeProfiles.containsKey(routePrefKey))
			return null;
		else {
			RoutingProfile rp = m_routeProfiles.get(routePrefKey);
			return rp;
		}
	}

	private int getRoutePreferenceKey(int routePref, boolean chEnabled) {
		int key = routePref;
		if (chEnabled)
			key += 100;

		return key;
	}
	
	public void printStatistics(Logger logger)
	{
		logger.info("====> Memory usage by profiles:");
		long totalUsedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		long totalProfilesMemory = 0;
		
		int i = 0;
		for(RoutingProfile profile : getUniqueProfiles())
		{
			i++;
			long capacity = profile.getCapacity();
			totalProfilesMemory += capacity;
			logger.info(String.format("[%d] %s (%.1f%%)", i, RuntimeUtility.getMemorySize(capacity), ((double)capacity/totalUsedMemory)*100)); 
		}
		
		logger.info(String.format("Total: %s (%.1f%%)", RuntimeUtility.getMemorySize(totalProfilesMemory), ((double)totalProfilesMemory/totalUsedMemory)*100)); 
		
		logger.info("========================================================================");
	}
}
