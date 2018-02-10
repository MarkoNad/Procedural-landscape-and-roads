package controller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.lwjgl.util.vector.Vector3f;

import entities.Entity;
import models.TexturedModel;
import models.TexturedModelComp;
import terrains.TreePlacer;
import terrains.TreeType;
import toolbox.QueueProduct;

public class LODGrid {
	
	private static final Logger LOGGER = Logger.getLogger(LODGrid.class.getName());
	
	private float cellSize;
	private Map<Point, Map<TreeType, List<Vector3f>>> grid;
	private Map<TexturedModelComp, Float> scaleForModel;
	private Map<TreeType, NavigableMap<Float, TexturedModelComp>> lodLevelsForType;
	
	public LODGrid(
			float cellSize,
			Map<TexturedModelComp, Float> scaleForModel,
			Map<TreeType, NavigableMap<Float, TexturedModelComp>> lodLevelsForType
	) {
		checkArgs(scaleForModel, lodLevelsForType);
		this.cellSize = cellSize;
		this.scaleForModel = scaleForModel;
		this.lodLevelsForType = lodLevelsForType;
		grid = new ConcurrentHashMap<>();
	}
	
	private void checkArgs(Map<TexturedModelComp, Float> scaleForModel,
			Map<TreeType, NavigableMap<Float, TexturedModelComp>> lodLevelsForType) {
		// checks if all models with lods have defined scale, and if all models with defined
		// scale have defined lods
		
		List<TexturedModelComp> modelsWithLODs = lodLevelsForType
				.values()
				.stream()
				.flatMap(m -> m.values().stream())
				.collect(Collectors.toList());
		List<TexturedModelComp> modelsWithScales = scaleForModel
				.keySet()
				.stream()
				.collect(Collectors.toList());
		
		for(TexturedModelComp model : modelsWithLODs) {
			if(!modelsWithScales.contains(model)) {
				throw new IllegalArgumentException("Undefined scale for model: " + model);
			}
			modelsWithScales.remove(model);
		}
		
		for(TexturedModelComp model : modelsWithScales) {
			if(!modelsWithLODs.contains(model)) {
				throw new IllegalArgumentException("Undefined LOD for model: " + model);
			}
		}
	}

	public void addToGrid(TreeType type, Vector3f location) {
		Point index = index(location);
		
		Map<TreeType, List<Vector3f>> cellMap = grid.get(index);
		if(cellMap == null) {
			cellMap = new ConcurrentHashMap<>();
			grid.put(index, cellMap);
		}
		
		List<Vector3f> locations = cellMap.get(type);
		if(locations == null) {
			locations = new CopyOnWriteArrayList<>();
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
	
	public void addToGrid(BlockingQueue<QueueProduct<Map<TreeType, List<Vector3f>>>> locationsPerTypeQueue, ExecutorService executor) {
		executor.submit(new Runnable() {
			@Override
			public void run() {
				while(true) {
					Map<TreeType, List<Vector3f>> locationsPerType = null;
					try {
						QueueProduct<Map<TreeType, List<Vector3f>>> product = locationsPerTypeQueue.take();
						
						if(product == TreePlacer.THREAD_POISON) {
							LOGGER.log(Level.FINE, "LOD grid received POISON.");
							break;
						}
						
						locationsPerType = product.getValue();
						
						LOGGER.log(Level.FINE, "Grid taken from queue " +
								locationsPerType.values().stream().mapToInt(l -> l.size()).sum() +
								" points. Queue size: " + locationsPerTypeQueue.size());
					} catch (InterruptedException ex) {
						LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
						System.exit(1);
					}
					
					for(TreeType type : locationsPerType.keySet()) {
						for(Vector3f location : locationsPerType.get(type)) {
							addToGrid(type, location);
						}
					}
				}
			}
		});
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
					NavigableMap<Float, TexturedModelComp> lodLevels = lodLevelsForType.get(type);
					Map.Entry<Float, TexturedModelComp> entry = lodLevels.ceilingEntry(distance);
					
					if(entry == null) continue; // type's render distance is too small
					
					TexturedModelComp compModel = entry.getValue();
					for(TexturedModel model : compModel.children) {
						//float scale = scaleForModel.get(model);
						float scale = scaleForModel.get(compModel);
						entities.add(new Entity(model, location, 0, 0, 0, scale));
					}
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

