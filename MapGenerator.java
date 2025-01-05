import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public enum MapGenerator {
    ;

    private static final List<Integer> NUMBER_COUNTERS = List.of(
        2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12);
    private static final Map<Integer, Integer> NUMBER_POINTS = createNumberPoints();
    private static final List<Tile> TILES = createTiles();
    private static final Set<Intersection> INTERSECTIONS = createThreeWayIntersections(TILES);
    private static final Map<Tile, List<Intersection>> TILES_TO_INTERSECTIONS = createTileToIntersections(INTERSECTIONS);
    private static final List<ResourceType> RESOURCES = createResources();
    private static final Map<ResourceType, Integer> EXPECTED_RESOURCE_DISTRIBUTION = Map.of(
        ResourceType.WHEAT, 16,
        ResourceType.SHEEP, 9,
        ResourceType.WOOD, 12,
        ResourceType.BRICK, 11,
        ResourceType.ORE, 10);

    public static Map<Tile, ResourceType> generateOptimalResourceDistribution(Map<Tile, Integer> tilesToNumbers, int iterations) {
        double bestScore = 0;
        Map<Tile, ResourceType> bestDist = null;
        for (int __ = 0; __ < iterations; ++__) {
            Map<Tile, ResourceType> newDist = generateResourceDistributionGreedily(tilesToNumbers);
            double newScore = calculateScore(newDist, tilesToNumbers);

            if (newScore > bestScore) {
                bestScore = newScore;
                bestDist = newDist;
            }
        }

        return bestDist;
    }

    public static Map<Tile, ResourceType> generateResourceDistributionGreedily(Map<Tile, Integer> tilesToNumbers) {
        Map<Tile, Integer> resourceTileNumbers = tilesToNumbers.entrySet().stream()
            .filter(e -> e.getValue() != 7)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        List<Tile> resourceTiles = new ArrayList<>(resourceTileNumbers.keySet());

        Map<Tile, ResourceType> tilesToResources = new HashMap<>();
        List<ResourceType> resources = new ArrayList<>(RESOURCES);
        Collections.shuffle(resources);

        for (int i = 0; i < resources.size(); ++i) {
            ResourceType r = resources.get(i);
            Tile t = resourceTiles.get(i);
            tilesToResources.put(t, r);
        }

        boolean isOptimised = false;
        while (!isOptimised) {
            double maxScore = calculateScore(tilesToResources, tilesToNumbers);
            int numResources = resources.size();
            isOptimised = true;
            for (int i = 0; i < numResources; ++i) {
                for (int j = i + 1; j < numResources; ++j) {
                    Tile t1 = resourceTiles.get(i);
                    Tile t2 = resourceTiles.get(j);
                    ResourceType r1 = tilesToResources.get(t1);
                    ResourceType r2 = tilesToResources.get(t2);
                    tilesToResources.replace(t1, r2);
                    tilesToResources.replace(t2, r1);
                    double swappedResourcesScore = calculateScore(tilesToResources, tilesToNumbers);

                    if (swappedResourcesScore > maxScore) {
                        isOptimised = false;
                        break;
                    } else {
                        tilesToResources.replace(t1, r1);
                        tilesToResources.replace(t2, r2);
                    }
                }

                if (!isOptimised) {
                    break;
                }
            }
        }

        return tilesToResources;
    }

    public static Map<Tile, Integer> generateOptimalNumberDistribution(int iterations) {
        double bestScore = Double.POSITIVE_INFINITY;
        Map<Tile, Integer> bestDist = null;
        for (int __ = 0; __ < iterations; ++__) {
            Map<Tile, Integer> newDist = generateNumberDistributionGreedily();
            double newScore = calculateScore(newDist, MapGenerator::variance, true);
            if (newScore < bestScore) {
                bestScore = newScore;
                bestDist = newDist;
            }
        }

        return bestDist;
    }

    private static Map<Tile, Integer> generateNumberDistributionGreedily() {
        Map<Tile, Integer> tilesToNumbers = new HashMap<>();
        List<Integer> numbersToPlace = new ArrayList<>(NUMBER_COUNTERS);
        Collections.shuffle(numbersToPlace);
        for (int i = 0; i < numbersToPlace.size(); ++i) {
            int num = numbersToPlace.get(i);
            Tile t = TILES.get(i);
            tilesToNumbers.put(t, num);
        }

        boolean isOptimised = false;
        while (!isOptimised) {
            double minScore = calculateScore(tilesToNumbers, MapGenerator::variance, true);
            int numTiles = TILES.size();

            isOptimised = true;
            for (int i = 0; i < numTiles; ++i) {
                for (int j = i + 1; j < numTiles; ++j) {
                    Tile t1 = TILES.get(i);
                    Tile t2 = TILES.get(j);
                    Integer t1Num = tilesToNumbers.get(t1);
                    Integer t2Num = tilesToNumbers.get(t2);
                    tilesToNumbers.replace(t1, t2Num);
                    tilesToNumbers.replace(t2, t1Num);
                    double swappedNumsScore = calculateScore(tilesToNumbers, MapGenerator::variance, true);
                    
                    if (swappedNumsScore < minScore) {
                        isOptimised = false;
                        break;
                    } else {
                        tilesToNumbers.replace(t1, t1Num);
                        tilesToNumbers.replace(t2, t2Num); 
                    }
                }

                if (!isOptimised) {
                    break;
                }
            }
        }

        return tilesToNumbers;
    }

    private static double calculateScore(Map<Tile, Integer> tilesToNumbers, ToDoubleFunction<Collection<Double>> scoringFunction, boolean applyPenalty) {
        Map<Intersection, Double> intersectionPoints = new HashMap<>();
        Map<Intersection, Set<Integer>> intersectionsToTiles = new HashMap<>();

        tilesToNumbers.forEach((tile, num) ->
            TILES_TO_INTERSECTIONS.get(tile).forEach(it -> {
                if (!intersectionsToTiles.computeIfAbsent(it, __ -> new HashSet<>()).contains(num)) {
                    intersectionPoints.merge(it, NUMBER_POINTS.get(num).doubleValue(), Double::sum);
                    intersectionsToTiles.get(it).add(num);
                }
            }));
            
        // remove intersections next to a 7
        Tile sevenTile = tilesToNumbers.entrySet().stream()
            .filter(entry -> entry.getValue() == 7)
            .map(Entry::getKey)
            .findAny()
            .get();

        TILES_TO_INTERSECTIONS.get(sevenTile).forEach(intersectionPoints::remove);

        // penalise if same number is on two adjacent tiles
        int penalty = 0;
        Map<Integer, Set<Tile>> numsToTiles = new HashMap<>();
        for (var e : tilesToNumbers.entrySet()) {
            Tile t = e.getKey();
            int num = e.getValue();
            numsToTiles.computeIfAbsent(num, __ -> new HashSet<>()).add(t);
        }
        for (Set<Tile> ts : numsToTiles.values()) {
            List<Tile> tsList = new ArrayList<>(ts);
            int tsCount = tsList.size();
            for (int i = 0; i < tsCount; ++i) {
                for (int j = i + 1; j < tsCount; ++j) {
                    Tile t1 = tsList.get(i);
                    Tile t2 = tsList.get(j);
                    if (t1.isAdjacent(t2)) {
                        penalty += 100;
                    }
                }
            }
        }
        
        return scoringFunction.applyAsDouble(intersectionPoints.values()) + (applyPenalty ? penalty : 0);
    }

    private static double calculateScore(Map<Tile, ResourceType> tilesToResources, Map<Tile, Integer> tilesToNums) {
        Map<ResourceType, Integer> resourcePoints = new HashMap<>();
        tilesToResources.forEach((t, r) -> {
            resourcePoints.merge(r, NUMBER_POINTS.get(tilesToNums.get(t)), Integer::sum);
        });

        int dotProduct = resourcePoints.entrySet().stream()
            .mapToInt(e -> {
                ResourceType r = e.getKey();
                return e.getValue() * EXPECTED_RESOURCE_DISTRIBUTION.get(r);
            })
            .sum();

        // penalise if same resource is on adjacent tiles
        // penalise if a resource has two of the same number tiles on it
        double penalty = 0;
        Map<ResourceType, Set<Tile>> resToTiles = new HashMap<>();
        for (var e : tilesToResources.entrySet()) {
            Tile t = e.getKey();
            ResourceType r = e.getValue();
            resToTiles.computeIfAbsent(r, __ -> new HashSet<>()).add(t);
        }
        for (Set<Tile> ts : resToTiles.values()) {
            List<Tile> tsList = new ArrayList<>(ts);
            int tsCount = tsList.size();
            for (int i = 0; i < tsCount; ++i) {
                for (int j = i + 1; j < tsCount; ++j) {
                    Tile t1 = tsList.get(i);
                    Tile t2 = tsList.get(j);
                    if (t1.isAdjacent(t2)) {
                        penalty += 0.0001;
                    }
                }
            }
            Set<Integer> nums = new HashSet<>();
            for (Tile t : tsList) {
                int num = tilesToNums.get(t);
                if (nums.contains(num)) {
                    penalty += 0.01;
                }

                nums.add(num);
            }
        }

        return dotProduct / norm(resourcePoints.values()) / norm(EXPECTED_RESOURCE_DISTRIBUTION.values()) - penalty;
    }

    private static double norm(Collection<Integer> vector) {
        return Math.sqrt(vector.stream().mapToDouble(x -> x * x).sum());
    }

    private static double variance(Collection<Double> nums) {
        double mean = nums.stream().mapToDouble(x -> x).sum() * 1.0 / nums.size();
        double result = 0;
        for (double x : nums) {
            result += (x - mean) * (x - mean);
        }

        return result / nums.size();
    }

    private static List<Tile> createTiles() {
        List<Tile> tiles = new ArrayList<>();
        for (int i = -2; i <= 2; ++i) {
            for (int j = -2; j <= 2; ++j) {
                for (int k = -2; k <= 2; ++k) {
                    if (i + j + k == 0) {
                        tiles.add(new Tile(i, j, k));
                    }
                }
            }
        }

        return tiles;
    }
    
    private static Set<Intersection> createThreeWayIntersections(List<Tile> tiles) {
        Set<Intersection> intersections = new HashSet<>();
        int numTiles = tiles.size();
        for (int i = 0; i < numTiles; ++i) {
            for (int j = i + 1; j < numTiles; ++j) {
                for (int k = j + 1; k < numTiles; ++k) {
                    Tile t1 = tiles.get(i);
                    Tile t2 = tiles.get(j);
                    Tile t3 = tiles.get(k);
                    if (t1.isAdjacent(t2) && t2.isAdjacent(t3) && t3.isAdjacent(t1)) {
                        intersections.add(new Intersection(t1, t2, t3));
                    }
                }
            }
        }

        return intersections;
    }

    private static Map<Tile, List<Intersection>> createTileToIntersections(Set<Intersection> intersections) {
        Map<Tile, List<Intersection>> result = new HashMap<>();
        intersections.forEach(it -> {
            for (Tile t : List.of(it.t1, it.t2, it.t3)) {
                result.computeIfAbsent(t, __ -> new ArrayList<>()).add(it);
            }
        });

        return result;
    }

    private static Map<Integer, Integer> createNumberPoints() {
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 2; i <= 6; ++i) {
            result.put(i, i - 1);
        }
        for (int i = 8; i <= 12; ++i) {
            result.put(i, 13 - i);
        }
        result.put(7, 0);
        return result;
    }

    private static List<ResourceType> createResources() {
        return Arrays.stream(ResourceType.values())
            .flatMap(r -> IntStream.rangeClosed(1, r.getFreq()).mapToObj(__ -> r))
            .toList();
    }

    private static record Tile(int x, int y, int z) {
        private static final Set<Integer> DIST_1 = Set.of(-1, 0, 1);
        boolean isAdjacent(Tile other) {
            Set<Integer> diffs = new HashSet<>();
            diffs.add(x - other.x);
            diffs.add(y - other.y);
            diffs.add(z - other.z);
            return diffs.equals(DIST_1);
        }
    }

    private static record Intersection(Tile t1, Tile t2, Tile t3) {}

    private static enum ResourceType {
        WHEAT(4, "WH"),
        WOOD(4, "WO"),
        SHEEP(4, "SH"),
        ORE(3, "OR"),
        BRICK(3, "BR")
        ;

        private final int freq;
        private final String image;

        private ResourceType(int freq, String image) {
            this.freq = freq;
            this.image = image;
        }

        private int getFreq() {
            return freq;
        }
        
        private String getImage() {
            return image;
        }
    }

    public static void main(String[] args) {
        Map<Tile, Integer> tilesMap = generateOptimalNumberDistribution(1000);
        Map<Tile, ResourceType> resourcesMap = generateOptimalResourceDistribution(tilesMap, 1000);

        String[][] grid = new String[14][18];
        for (String[] row : grid) {
            Arrays.fill(row, " ");
        }

        int midX = 6;
        int midY = 8;

        tilesMap.forEach((tile, num) -> {
            int tileX = midX + tile.x * -1 + tile.y * -1 + tile.z * 2;
            int tileY = midY + tile.x * 2 + tile.y * -2;

            String numStr = num == 7 ? " " : String.valueOf(num);
            for (int i = 0; i < numStr.length(); ++i) {
                grid[tileX][tileY + i] = numStr.substring(i, i + 1);
            }

            ResourceType res = resourcesMap.get(tile);
            String resStr = res != null ? res.getImage() : "--";
            for (int i = 0; i < resStr.length(); ++i) {
                grid[tileX + 1][tileY + i] = resStr.substring(i, i + 1);
            }
        });

        for (String[] row : grid) {
            for (String c : row) {
                System.out.printf(c);
            }

            System.out.println("");
        }

        int minIntersectionPoint = (int) calculateScore(tilesMap, Collections::min, false);
        int maxIntersectionPoint = (int) calculateScore(tilesMap, Collections::max, false);

        Map<ResourceType, Integer> resourceDist = new HashMap<>();
        resourcesMap.forEach((t, r) -> resourceDist.merge(r, NUMBER_POINTS.get(tilesMap.get(t)), Integer::sum));

        System.out.printf("Number tiles score: %f\n", calculateScore(tilesMap, MapGenerator::variance, true));
        System.out.printf("Intersection points range: [%d, %d]\n", minIntersectionPoint, maxIntersectionPoint);
        System.out.println(resourceDist);
        System.out.printf("Resources distribution score: %f\n", calculateScore(resourcesMap, tilesMap));
    }
}