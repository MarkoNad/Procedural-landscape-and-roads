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
	//private Map<TreeType, TexturedModel> modelForType;
	private Map<TexturedModel, Float> scaleForModel;
	private Map<TreeType, NavigableMap<Float, TexturedModel>> lodLevelsForType;
	
	public LODGrid(
			float cellSize,
			//Map<TreeType, TexturedModel> modelForType,
			Map<TexturedModel, Float> scaleForModel,
			Map<TreeType, NavigableMap<Float, TexturedModel>> lodLevelsForType
	) {
		this.cellSize = cellSize;
		//this.modelForType = modelForType;
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
		
		//List<Point> nearCells = cellsToCheck(position);
		//System.out.println(grid.entrySet().size());
		//System.out.println(nearCells.size());
		
		for(Map.Entry<Point, Map<TreeType, List<Vector3f>>> cellEntry : grid.entrySet()) {
		//for(Point cellCoords : nearCells) {
			Point cellCoords = cellEntry.getKey();
			if(tooFar(cellCoords, index(position), searchRange));
			Map<TreeType, List<Vector3f>> cellData = cellEntry.getValue();
			//Map<TreeType, List<Vector3f>> cellData = grid.get(cellCoords);
			if(cellData == null) continue; // cell is outside of grid, no objects at that location
			float cellDistance = minimalCellXZDistance(cellCoords, position);
			Set<TreeType> typesToRender = new HashSet<>();
			for(TreeType candidateType : cellData.keySet()) {
				float typeMaxRenderDistance = lodLevelsForType.get(candidateType).lastKey();
				//if(typeMaxRenderDistance + cellSize * Math.sqrt(2) / 2.0> cellDistance) {
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
					if(entry == null) continue;
					TexturedModel model = lodLevels.ceilingEntry(distance).getValue();
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
	
	private List<Point> cellsToCheck(Vector3f position) {
		List<Point> cells = new ArrayList<>();

		// render distance is the maximum distance at which any of the types is still rendered
		float renderDistance = 0;
		for(TreeType type : lodLevelsForType.keySet()) {
			float typeRenderDistance = lodLevelsForType.get(type).lastKey();
			if(typeRenderDistance > renderDistance) renderDistance = typeRenderDistance;
		}
		
		Point positionCell = index(position);
		int range = (int) (renderDistance / cellSize + 1);
		
		for(int y = positionCell.y - range; y <= positionCell.y + range; y++) {  // y is actually z here
			for(int x = positionCell.x - range; x <= positionCell.x + range; x++) {
				Point candidateCell = new Point(x, y);
				if(grid.containsKey(candidateCell)) cells.add(candidateCell);
			}
		}
		
		return cells;
	}
	
	private Point index(Vector3f location) {
		return new Point(Math.round(location.x / cellSize), Math.round(location.z / cellSize));
	}
	
	private float minimalCellXZDistance(Point cell, Vector3f position) {
		return (float) Math.max(0, Math.hypot(position.x - cell.x, position.y - cell.y) - cellSize * Math.sqrt(2) / 2.0);
	}

}
