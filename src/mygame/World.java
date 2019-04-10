/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.HashMap;
import javax.vecmath.Point2i;
import javax.vecmath.Tuple2i;

public class World {

    private Vector3f startPos;
    public Vector3f endPos;
    private Node rootNode;
    private HashMap<Tuple2i, WorldBlock> blocks; // (X,Z) mapping to world blocks.
    private WorldGen worldGen;
    
    
    private Node nonCollidables;

    public World(Node rootNode) {
        this.rootNode = rootNode;
        worldGen = new WorldGen();
        blocks = new HashMap();
        
        nonCollidables = new Node("NonCollidables");
    }

    public void addBlockWithTorch(int posX, int posY, boolean[] walls) {
        WorldBlock b = new WorldBlock(walls);


        b.addTorch();
        PointLight torch_light = new PointLight();
        torch_light.setColor(ColorRGBA.Cyan.mult(1.1f));
        torch_light.setRadius(35f);
        torch_light.setPosition(new Vector3f(coordsBlockToWorld(posX, posY)));
        rootNode.addLight(torch_light);
        
        // Add Shadows code here


        b.getNode().setLocalTranslation(coordsBlockToWorld(posX, posY));
        rootNode.attachChild(b.getNode());

        blocks.put(new Point2i(posX, posY), b);
    }
    
    public void addEndBlock(int posX, int posY, boolean[] walls) {       
        
        WorldBlock b = new WorldBlock(walls, true);
        b.getNode().setLocalTranslation(coordsBlockToWorld(posX, posY));
        rootNode.attachChild(b.getNode());

        blocks.put(new Point2i(posX, posY), b);
        
        b.getNonCollidables().setLocalTranslation(coordsBlockToWorld(posX, posY));
        nonCollidables.attachChild(b.getNonCollidables());
        
        endPos = coordsBlockToWorld(posX, posY,2.5f);
        
    }

    public void addBlock(int posX, int posY, boolean[] walls) {
        WorldBlock b = new WorldBlock(walls);
        b.getNode().setLocalTranslation(coordsBlockToWorld(posX, posY));
        rootNode.attachChild(b.getNode());

        blocks.put(new Point2i(posX, posY), b);
    }

    public WorldBlock getBlock(int posX, int posY) {
        return blocks.get(new Point2i(posX, posY));
    }

    public WorldBlock getBlock(Tuple2i pos) {
        return blocks.get(pos);
    }

    public Vector3f getStartPos() {
        return startPos;
    }
    
    public Node getNonCollidables() {
        return nonCollidables;
    }

    public int getBlockTypeAt(Vector3f pos) {
        int[][] grid = worldGen.getMap();
        Tuple2i blockCoords = coordsWorldToBlock(pos);
        return grid[blockCoords.x+WorldGen.GRID_HALFSIZE][blockCoords.y+WorldGen.GRID_HALFSIZE];
    }
    
    public void generateWorld() {
        worldGen.createRooms();
        //worldGen.createTorches();
        //startPos = worldGen.getStartPos();

        int[][] tiles = worldGen.getMap();

        int startX = -WorldGen.GRID_SIZE / 2, startY = -WorldGen.GRID_SIZE / 2; // Block start positions
        for (int x = 0; x < WorldGen.GRID_SIZE; x++) {
            for (int y = 0; y < WorldGen.GRID_SIZE; y++) {
                if (tiles[x][y] == WorldGen.ROOM) {
                    boolean[] walls = new boolean[4];

                    // Check 4 surrounding areas for walls
                    walls[1] = (y > 0 && tiles[x][y - 1] == WorldGen.WALL); // North wall (Actually East)
                    walls[0] = (y < WorldGen.GRID_SIZE - 1 && tiles[x][y + 1] == WorldGen.WALL); // South wall (so probably West)
                    walls[3] = (x < WorldGen.GRID_SIZE - 1 && tiles[x + 1][y] == WorldGen.WALL); // East wall (probably North)
                    walls[2] = (x > 0 && tiles[x - 1][y] == WorldGen.WALL); // West wall (probably South)

                    addBlock(startX + x, startY + y, walls);

                    if (startPos == null) {
                        //startPos = new Vector3f(0f, 10f, 0f);
                        startPos = coordsBlockToWorld(startX + x, startY + y, 5f);
                        //startPos.x += WorldBlock.SIZE;
                        //startPos.z -= WorldBlock.SIZE;
                        //System.out.println("Start: " + (startX + x) + ", " + (startY + y));
                        worldGen.createDestinationRoom(startX + x, startY + y);
                    }
                }
                else if (tiles[x][y] == WorldGen.END) {
                    boolean[] walls = new boolean[4];
                    // End portal only on the west wall, the others make a box.
                    walls[0] = walls[1] = walls[3] = true;
                    
                    
                    
                    addEndBlock(startX + x, startY + y, walls); // When portal material is ready, walls[2] is made of that
                }
            }
        }
        
        //worldGen.printWorld();
    }
    
    public Vector3f pseudoRandomRoomPos(Vector3f pos){
        
//        worldGen.getSoundOrigin(coordsWorldToBlock(pos));
        
        Vector3f dir = new Vector3f(FastMath.nextRandomFloat(),0,FastMath.nextRandomFloat());
        dir.mult(FastMath.nextRandomInt(10, 20));
        

        return dir;
    }

    // Transforms block coordinates (int, int) into world coordinates (float, float).
    public static Vector3f coordsBlockToWorld(int blockX, int blockY) {
        return new Vector3f(blockX * WorldBlock.SIZE, 0f, blockY * WorldBlock.SIZE);
    }

    public static Vector3f coordsBlockToWorld(int blockX, int blockY, float height) {
        return new Vector3f(blockX * WorldBlock.SIZE, height, blockY * WorldBlock.SIZE);
    }
   

    public static Tuple2i coordsWorldToBlock(Vector3f worldCoords) {
        float valX = (worldCoords.x + WorldBlock.SIZE_HALF) / WorldBlock.SIZE;
        float valY = (worldCoords.z + WorldBlock.SIZE_HALF) / WorldBlock.SIZE;

        if (valX < 0) {
            --valX;
        }
        if (valY < 0) {
            --valY;
        }

        return new Point2i((int) valX, (int) valY);
    }

}
