package controller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import org.lwjgl.util.vector.Vector3f;

import entities.Entity;
import models.TexturedModel;
import terrains.BiomesMap.TreeType;

public class LODGrid {
	
	private float cellSize;
	private Map<Point, Map<TreeType, List<Vector3f>>> grid;
	private Map<TexturedModel, Float> scaleForModel;
	private Map<TreeType, NavigableMap<Float, TexturedModel>> lodLevelsForType;
	
	public LODGrid(
			float cellSize,
			Map<TexturedModel, Float> scaleForModel,
			Map<TreeType, NavigableMap<Float, TexturedModel>> lodLevelsForType
	) {
		this.cellSize = cellSize;
		this.scaleForModel = scaleForModel;
		this.lodLevelsForType = lodLevelsForType;
		grid = new HashMap<>();
	}
	
	public void addToGrid(TreeType type, Vector3f location) {
		Point index = index(location);
		
		Map<TreeType, List<Vector3f>> cellMap = grid.get(index);
		if(cellMap == null) {
			cellMap = new HashMap<>();
			grid.put(index, cellMap);
		}
		
		List<Vector3f> locations = cellMap.get(type);
		if(locations == null) {
			locations = new ArrayList<>();
			cellMap.put(type, locations);
		}
		
		locations.add(location);
	}

	public void addToGrid(Map<TreeType, List<Vector3f>> locationsPerType) {
		for(TreeType type : locationsPerType.keySet()) {
			for(Vector3f location : locationsPerType.get(type)) {
				addToGrid(type, location);
			}
		}
	}
	
	public List<Entity> proximityEntities(Vector3f position) {
		List<Entity> entities = new ArrayList<>();
		int searchRange = searchRange();
		
		// traverse all cells, but discard those that are too far
		for(Map.Entry<Point, Map<TreeType, List<Vector3f>>> cellEntry : grid.entrySet()) {
			Point cellCoords = cellEntry.getKey();
			if(tooFar(cellCoords, index(position), searchRange)) continue;
			
			Map<TreeType, List<Vector3f>> cellData = cellEntry.getValue();
			if(cellData == null) continue; // cell is in range but outside of grid, no objects at that location
			
			// determine what types are in the cell, types not present will not be even considered to render afterwards
			float cellDistance = minimalCellXZDistance(cellCoords, position);
			Set<TreeType> typesToRender = new HashSet<>();
			for(TreeType candidateType : cellData.keySet()) {
				float typeMaxRenderDistance = lodLevelsForType.get(candidateType).lastKey();
				if(typeMaxRenderDistance > cellDistance) {
					typesToRender.add(candidateType);
				}
			}

			for(TreeType type : typesToRender) {
				for(Vector3f location : cellData.get(type)) {
					float distance = (float) Math.sqrt(Math.pow(position.x - location.x, 2) + 
							Math.pow(position.y - location.y, 2) + Math.pow(position.z - location.z, 2));
					NavigableMap<Float, TexturedModel> lodLevels = lodLevelsForType.get(type);
					Map.Entry<Float, TexturedModel> entry = lodLevels.ceilingEntry(distance);
					
					if(entry == null) continue; // type's render distance is too small
					
					TexturedModel model = entry.getValue();
					float scale = scaleForModel.get(model);
					entities.add(new Entity(model, location, 0, 0, 0, scale));
				}
			}
		}
		
		return entities;
	}
	
	private boolean tooFar(Point cell, Point positionCell, int range) {
		return Math.abs(cell.x - positionCell.x) > range || (Math.abs(cell.y - positionCell.y)) > range;
	}
	
	private int searchRange() {
		// render distance is the maximum distance at which any of the types is still rendered
		float renderDistance = 0;
		for(TreeType type : lodLevelsForType.keySet()) {
			float typeRenderDistance = lodLevelsForType.get(type).lastKey();
			if(typeRenderDistance > renderDistance) renderDistance = typeRenderDistance;
		}
		return (int) (renderDistance / cellSize + 1);
	}
	
	private Point index(Vector3f location) {
		return new Point(Math.round(location.x / cellSize), Math.round(location.z / cellSize));
	}
	
	/**
	 * Distance from position projected to x-z plane from the cell bounding circle.
	 * 
	 * @param cell Coordinates of the cell center.
	 * @param position Position whose distance to cell bounding circle is calculated.
	 * @return Distance from position projected to x-z plane from the cell bounding circle.
	 */
	private float minimalCellXZDistance(Point cell, Vector3f position) {
		return (float) Math.max(0, Math.hypot(position.x - cell.x * cellSize, position.z - cell.y * cellSize) - cellSize * Math.sqrt(2) / 2.0);
	}

}
