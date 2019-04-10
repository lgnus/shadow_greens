package mygame;

import java.util.Random;
import javax.vecmath.Point2i;
import javax.vecmath.Tuple2i;

public class WorldGen {

    // Main options
    public static final int GRID_HALFSIZE = 25;
    public static final int GRID_SIZE = GRID_HALFSIZE * 2;
    private static final int LEVEL_END_THRESHOLD = GRID_SIZE * 2/3;
    private static final int ROOMS = 25;
    private static final int ROOM_FIELDS = 4; // x,y, width, height
    private static final int MIN_SIZE = 4; // 3 and up
    private static final int MAX_SIZE = 6;
    //
    public static final int WALL = 0;
    public static final int ROOM = 1;
    public static final int TORCH = 2;
    public static final int END = 3;
    // Room fields
    private static final int SX = 0;
    private static final int SY = 1;
    private static final int EX = 2;
    private static final int EY = 3;
    private static final Random rng = new Random();
    private int grid[][];
    private int rooms[][]; 

    public WorldGen() {

        // Initializes
        grid = new int[GRID_SIZE][GRID_SIZE];
        rooms = new int[ROOMS][ROOM_FIELDS];

        //createRooms();
    }

    /**
     * Creates randomly generated rooms with random size and assigns them to the
     * world if they don't stack on each other
     *
     */
    public void createRooms() {

        // Creates random rooms and if they are allowed
        // add them to the rooms array
        for (int i = 0; i < ROOMS; i++) {
            int x, y;
            int[] room = {x = rng.nextInt(GRID_SIZE - MAX_SIZE) + 1, y = rng.nextInt(GRID_SIZE - MAX_SIZE) + 1,
                x + randRange(MIN_SIZE, MAX_SIZE), y + randRange(MIN_SIZE, MAX_SIZE)};

            // if it's allowed to add it to the structures
            if (allowed(room)) {
                room[EX]--;
                room[EY]--;
                rooms[i] = room;
            } else {
                i--;
            }
        }

        addRoomsToMap();
        addPathsToMap(getConnections());

    }
    

    public void createTorches() {
        // TODO: Better RNG
        for (int i = 0; i < rooms.length; i++) {
            int[] r = rooms[i];
            int rx = randRange(r[SX] + 1, r[EX] - 1);
            int ry = randRange(r[SY] + 1, r[EY] - 1);

            // New RNG
            if (rng.nextBoolean()) { // if along x wall
                if (rng.nextBoolean()) {
                    ry = r[SY];
                } else {
                    ry = r[EY]-1;
                }
            } else {
                if (rng.nextBoolean()) {
                    rx = r[SX];
                } else {
                    rx = r[EX]-1;
                }
            }

            grid[rx][ry] = WorldGen.TORCH;
        }
    }
    
    public Tuple2i getSoundOrigin(Tuple2i pos){
        
        
        
        
        return new Point2i(0,0);
    }
    
    /**
     * Turn one of the rooms into the destination given the start position specified
     * @param startPosX
     * @param startPosY 
     */
    public void createDestinationRoom(int startPosX, int startPosY) {
        // Select destination room that is far enough
        int[] room;
        boolean hollowCheck;
        do {
            room = rooms[rng.nextInt(rooms.length)];
            // Also check if the wall that'll take the end portal is solid
            hollowCheck = true;
            for (int i = room[SY]; i < room[EY]; i++)
            {
                if ((room[EX]+1 < grid.length) && grid[room[EX]+1][i] == ROOM)
                { // hollow wall
                    hollowCheck = false;
                    break;
                }
            }
        } while ((room[EX] < LEVEL_END_THRESHOLD) && !hollowCheck);
        
        // Select and mark destination wall
        int endWallLength = room[EY] - room[SY];
        int j = room[SY];
        int jStop = room[EY];
        
        // If the wall is longer than 3 units, the end doesn't take all of the wall
        if (endWallLength > 3) {
            j++;
            jStop--;
        }
        // Mark the end section of the wall
        for (; j < jStop; j++) {
            grid[room[EX]][j] = END;
        }
    }

    /**
     * Changes the rooms to have paths between the different rooms
     */
    private void addPathsToMap(int[] connections) {

        for (int i = 0; i < rooms.length; i++) {
            int[] r = rooms[connections[i]];

            int rx1 = randRange(rooms[i][SX] + 1, rooms[i][EX] - 1);
            int rx2 = randRange(r[SX] + 1, r[EX] - 1);
            int ry1 = randRange(rooms[i][SY] + 1, rooms[i][EY] - 1);
            int ry2 = randRange(r[SY] + 1, r[EY] - 1);

            while (rx1 != rx2 || ry1 != ry2) {
                if (rx1 != rx2) {
                    if (rx1 > rx2) {
                        rx1--;
                        grid[rx1][ry1] = ROOM;
                    } else if (rx2 > rx1) {
                        rx1++;
                        grid[rx1][ry1] = ROOM;
                    }
                } else if (ry1 != ry2) {
                    if (ry1 > ry2) {
                        ry1--;
                        grid[rx1][ry1] = ROOM;
                    } else if (ry2 > ry1) {
                        ry1++;
                        grid[rx1][ry1] = ROOM;
                    }
                }
            }

        }
    }

    /**
     * Creates a link between every room and its closest other room that wasn't
     * visited yet
     *
     * @return an array with the connections of rooms ( index, roomToConnect)
     */
    private int[] getConnections() {

        int[] connections = new int[rooms.length];
        boolean[] visited = new boolean[rooms.length];
        int curr = rng.nextInt(rooms.length);
        int next = 0; // TODO check mambos
        visited[curr] = true;

        // While there are still rooms to visit
        while (!allVisited(visited)) {

            double min_distance = Double.MAX_VALUE;
            // Find next step by picking the closest

            for (int i = 0; i < rooms.length; i++) {
                if (!visited[i]) {
                    int cx1 = rooms[curr][SX] + ((rooms[curr][EX] - rooms[curr][SX]) / 2);
                    int cx2 = rooms[i][SX] + ((rooms[i][EX] - rooms[i][SX]) / 2);
                    int cy1 = rooms[curr][SY] + ((rooms[curr][EY] - rooms[curr][SY]) / 2);
                    int cy2 = rooms[i][SY] + ((rooms[i][EY] - rooms[i][SY]) / 2);

                    double d = getDistance(cx1, cy1, cx2, cy2);

                    if (d < min_distance) {
                        min_distance = d;
                        next = i;
                    }
                }

                // Assign next move thingy
                connections[curr] = next;
                curr = next;
                visited[curr] = true;
            }
        }
        return connections;
    }

    private boolean allVisited(boolean[] visited) {
        for (boolean b : visited) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    private double getDistance(int x1, int y1, int x2, int y2) {
        return ((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    /**
     * Changes the rooms to have paths between the different rooms
     */
    private void addRoomsToMap() {
        // Puts all the rooms into the grid
        for (int[] r : rooms) {
            for (int i = r[SX]; i < r[EX]; i++) {
                for (int j = r[SY]; j < +r[EY]; j++) {

                    // Assign Horizontal walls
                    if (i == r[SX] && grid[i - 1][j] != ROOM) {
                        grid[i - 1][j] = WALL;
                    }

                    if (i == r[EX] - 1 && grid[i + 1][j] != ROOM) {
                        grid[i + 1][j] = WALL;
                    }

                    // Assign Vertical walls
                    if (j == r[SY] && grid[i][j - 1] != ROOM) {
                        grid[i][j - 1] = WALL;
                    }

                    if (j == r[EY] - 1 && grid[i][j + 1] != ROOM) {
                        grid[i][j + 1] = WALL;
                    }

                    grid[i][j] = ROOM;
                }
            }

        }
    }

    /**
     * @param room - room to be inspected
     * @return - true if no other room already exists at this location, false
     * otherwise
     */
    private boolean allowed(int[] room) {

        for (int[] cur : rooms) {

            if (!(room[EX] < cur[SX] - 1 || room[SX] - 1 > cur[EX] || room[EY] < cur[SY] - 1
                    || room[SY] - 1 > cur[EY])) {
                return false;
            }
        }
        return true;
    }

    public int[][] getMap() {
        return grid;
    }

    /**
     * Displays
     */
    public void printWorld() {
        for (int i = GRID_SIZE-1; i >= 0; i--) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int x = grid[i][j];
                if (x == WALL) {
                    System.out.print("0 ");
                } else if (x == END) {
                    System.out.print("1 ");
                } else if (x == ROOM) {
                    System.out.print("  ");
                } else {
                    System.out.println("X ");
                }

            }
            System.out.println();
        }
    }

    /**
     * Generates a random number in a certain range(both included)
     *
     * @param min - min boundary included
     * @param max - max boundary
     * @return - int between min and max
     */
    private int randRange(int min, int max) {
        return min + rng.nextInt(max - min + 1);
    }

    public Tuple2i getStartPos() {
        int r = rng.nextInt(rooms.length);
        int rx = randRange(rooms[r][SX] + 1, rooms[r][EX] - 1);
        int ry = randRange(rooms[r][SY] + 1, rooms[r][EY] - 1);

        return new Point2i(rx, ry);
    }
    
//    public int[][] getRooms() {
//        return rooms;
//    }

    /**
     * Prints all the rooms fields
     */
    private void printRooms() {
        for (int i = 0; i < ROOMS; i++) {
            for (int c : rooms[i]) {
                System.out.print(c + " ");
            }
            System.out.print("\n");
        }
    }
    
    public static void main(String[] args) {
        WorldGen derp = new WorldGen();
        derp.createRooms();
        derp.createDestinationRoom(0, 0);
        derp.printRooms();
        System.out.println("\n----\n");
        derp.printWorld();
    }
}
