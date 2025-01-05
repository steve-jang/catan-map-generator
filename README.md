# Catan Map Generator
## Overview
###
A Java program that randomly generates a "fair" Catan map, where fairness is determined by the distribution and availability of resources, as well as the distribution of number tiles.

## Fairness
First, some definitions:
- **Intersection**: anywhere that you can place a settlement/city.
- **3-Intersection**: intersections adjacent to three resource tiles.
- **Quality** of an intersection: sum of the "probability dots" of the number tiles adjacent to the intersection. E.g. if an intersection was adjacent to 2, 5, and 8, then its quality is 1 + 4 + 5 = 10.
- **Availability** of a resource: sum of "probability dots" of unique tiles which supply the resource, where uniqueness is based on the number tile placed on the tile. E.g. if wheat is produced on tiles with numbers 3, 6, 8 and 8, its availability is 2 + 5 + 5 = 12.

The generator (tries to) do the following:
- First, minimise the variance of the quality of 3-Intersections, while avoiding to place the same number tile next to each other, then
- Make sure the distribution of resource availabilities matches some pre-defined "expected" distribution as closely as possible (controllable via `EXPECTED_RESOURCE_DISTRIBUTION`), while avoiding to place the same resource next to each other or on multiple tiles with the same number.

With this logic, a Catan map is "fair" if all intersections which are adjacent to three resource tiles have roughly the same probability of producing a resource (1), and if the distributon of resource probabilities for the overall map is close to what is required for a fun game of Catan (2). The reason for (1) is to have the most even playing field possible when placing settlements at the start of the game and avoid creating objectively bad intersections, and (2) is to prevent games with one or more resource in a constant shortage.