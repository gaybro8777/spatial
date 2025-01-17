/*
 * Copyright (c) 2010-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Spatial.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gis.spatial.pipes.processing;

import org.neo4j.gis.spatial.pipes.AbstractGeoPipe;
import org.neo4j.gis.spatial.pipes.GeoPipeFlow;

/**
 * Computes the minimum bounding box of item geometry.
 * Item geometry is replaced by pipe output unless an alternative property name is given in the constructor.
 */
public class Envelope extends AbstractGeoPipe {
	
	public Envelope() {
	}	
	
	/**
	 * @param resultPropertyName property name to use for geometry output
	 */	
	public Envelope(String resultPropertyName) {
		super(resultPropertyName);
	}	

	@Override	
	protected GeoPipeFlow process(GeoPipeFlow flow) {
		setGeometry(flow, flow.getGeometry().getEnvelope());
		return flow;
	}
}