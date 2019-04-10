/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.water.SimpleWaterProcessor;

public class WorldBlock {

    public static AudioNode portalSound;
    public static final float SIZE_HALF = 2f; // Width/Length of the block
    public static final float SIZE = SIZE_HALF * 2;
    public static final float HEIGHT = 2.5f; // Height of the block
    public static final float WALL_WIDTH = 0.05f; // Width of the block's wall
    private static Material materialFloor, materialWall, materialTorch;
    private static Box meshWall;
    private static Quad meshFloor;
    private static AssetManager assetManager;
    private static SimpleWaterProcessor waterProcessor;

    public static void load(AssetManager as, SimpleWaterProcessor waterProc) {
        assetManager = as;
        waterProcessor = waterProc;
        
        // Portal Sound Properties
        portalSound = new AudioNode(assetManager, "Sounds/portal.ogg");
        portalSound.stop();
        portalSound.setPositional(true);
        portalSound.setDirectional(true);
        portalSound.setLooping(true);
        portalSound.setRefDistance(10f);
        portalSound.setMaxDistance(100f);
        

        // Handle floor material
        materialFloor = assetManager.loadMaterial("Materials/grass.j3m");
        
        




        // Handle Wall material
//        materialWall = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        materialWall.setTexture("DiffuseMap",
//                assetManager.loadTexture("Textures/Wall/wallN.png"));
//        materialWall.setTexture("NormalMap",
//                assetManager.loadTexture("Textures/Wall/NormalMap.png"));
//        materialWall.setTexture("SpecularMap", 
//                assetManager.loadTexture("Textures/Wall/SpecularMap.png"));
//        materialWall.setTexture("ParallaxMap",
//                assetManager.loadTexture("Textures/Wall/DisplacementMap.png"));
//        materialWall.setBoolean("SteepParallax", true);
//        materialWall.setBoolean("PackedNormalParallax", true);
//        materialWall.setFloat("ParallaxHeight", .3f);
        materialWall = assetManager.loadMaterial("Materials/walls.j3m");





        // Handle Torch Material
        materialTorch = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        materialTorch.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));

        meshFloor = new Quad(SIZE, SIZE);
        meshWall = new Box(WALL_WIDTH, HEIGHT, SIZE_HALF);
        
        // Register water processor
        
    }
    private BatchNode node; // The node for this WorldBlock, contains the geometry
    private Geometry geoFloor; // Floor
    private Geometry geoWalls[] = new Geometry[4]; // [ N, S, E, W ] walls
    private Node nonCollidables;

    public WorldBlock(boolean walls[]) {
        this(walls, false);
    }
    
    // Constructor for End Portal blocks
    public WorldBlock(boolean[] walls, boolean isEnd) {
        node = new BatchNode("WorldBlock");
        nonCollidables = new Node("NonCollidables");

        geoFloor = new Geometry("Floor", meshFloor);
        geoFloor.rotate((float) -(Math.PI / 2), 0f, 0f);
        geoFloor.setMaterial(materialFloor);
        geoFloor.setLocalTranslation(-SIZE_HALF, -HEIGHT / 2 + 0.5f, SIZE_HALF);

        node.setShadowMode(ShadowMode.Receive);
        node.attachChild(geoFloor);
        


        Node wallsNode = new Node("Walls");

        // walls[] always has 4 sides (N, S, E, W) or is null
        if (walls != null && walls.length == 4) {
            // Add walls

            for (int x = 0; x < 4; x++) {
                if (!walls[x]) {
                    continue;
                }

                geoWalls[x] = new Geometry("Wall" + x, meshWall);
                geoWalls[x].setMaterial(materialWall);

                switch (x) {
                    default:
                    case 0: // North
                        geoWalls[x].rotate(0f, (float) (Math.PI / 2), 0f);
                        geoWalls[x].setLocalTranslation(0f, HEIGHT / 2 + WALL_WIDTH+0.475f, SIZE_HALF - WALL_WIDTH);
                        break;

                    case 1: // South
                        geoWalls[x].rotate(0f, (float) (Math.PI / 2), 0f);
                        geoWalls[x].setLocalTranslation(0f, HEIGHT / 2 + WALL_WIDTH+0.475f, -SIZE_HALF + WALL_WIDTH);
                        break;

                    case 2: // East
                        geoWalls[x].setLocalTranslation(-SIZE_HALF + WALL_WIDTH, HEIGHT / 2 + WALL_WIDTH+0.475f, 0f);
                        break;

                    case 3: // West
                        geoWalls[x].setLocalTranslation(SIZE_HALF - WALL_WIDTH, HEIGHT / 2 + WALL_WIDTH+0.475f, 0f);
                        break;
                }

                wallsNode.attachChild(geoWalls[x]);
                wallsNode.setShadowMode(ShadowMode.Cast);
            }
            
            if (isEnd && (waterProcessor != null)) {
            // Create portal effect
                Spatial waterPlane = waterProcessor.createWaterGeometry(SIZE, HEIGHT*2);
                waterPlane.rotate(0, FastMath.DEG_TO_RAD * -90f, FastMath.DEG_TO_RAD * 90f); // FastMath.DEG_TO_RAD * 90f
                waterPlane.setMaterial(waterProcessor.getMaterial());
                waterPlane.setLocalTranslation(-SIZE_HALF - .07f, HEIGHT*1.7f + WALL_WIDTH, SIZE_HALF);
                //waterPlane.move(0,-11,0);
                nonCollidables.attachChild(waterPlane);
                
                
                
            }
            node.attachChild(wallsNode);
        }
        
        
        node.batch();
    }

    public void addTorch() {
        geoFloor = new Geometry("Floor", meshFloor);
        geoFloor.rotate((float) -(Math.PI / 2), 0f, 0f);
        geoFloor.setMaterial(materialFloor);
        geoFloor.setLocalTranslation(-SIZE_HALF, -HEIGHT / 2 + 0.5f, SIZE_HALF);
        node.attachChild(geoFloor);

        ParticleEmitter fireEffect = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        fireEffect.setMaterial(materialTorch);
        fireEffect.setImagesX(2);
        fireEffect.setImagesY(2); // 2x2 texture animation
        fireEffect.setEndColor(new ColorRGBA(0f, 0f, 1f, 1f));   // red
        fireEffect.setStartColor(new ColorRGBA(0f, .3f, 1f, 0.5f)); // yellow
        fireEffect.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fireEffect.setStartSize(0.6f);
        fireEffect.setEndSize(0.1f);
        fireEffect.setGravity(0f, 0f, 0f);
        fireEffect.setLowLife(0.5f);
        fireEffect.setHighLife(3f);
        fireEffect.getParticleInfluencer().setVelocityVariation(0.3f);
        fireEffect.move(0, 1.5f, 0);
        node.attachChild(fireEffect);


    }

    public WorldBlock() {
        this(null);
    }
    

    public Geometry getFloor() {
        geoFloor = new Geometry("Floor", meshFloor);
        geoFloor.setLocalTranslation(0f, -HEIGHT / 2, 0f);
        return geoFloor;
    }

    public Node getNode() {
        return node;
    }
    
    public Node getNonCollidables() {
        return nonCollidables;
    }
}
