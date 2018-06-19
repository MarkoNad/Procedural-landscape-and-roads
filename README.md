# Procedural landscape and roads generator

This repository contains an Eclipse project with a procedural landscape and roads generator developed as part of my master's thesis.

The developed framework consists of two main parts:
*  A landscape renderer - displays either a synthetically generated terrain or a terrain reconstructed from a heightmap. The terrain is textured using user provided parameters, and vegetation is generated using specified models.
*  Road trajectory optimizer - generates a road that links the user specified initial and final points. A series of criteria and constraints, such as preference to moderate curvatures and slope limits, can be expressed. The trajectory is determined using search algorithms and road geometry is generated based on this trajectory.

Demo applications using this framework are available in the demo sub-package.