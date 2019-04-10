package mygame;

import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.audio.LowPassFilter;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.input.controls.ActionListener;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.CartoonEdgeFilter;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.texture.Texture;
import com.jme3.water.SimpleWaterProcessor;

/**
 *
 * @author José Carneiro, Miguel Cabrita, Pedro Almeida
 */
public class Main extends SimpleApplication implements ActionListener {

    public static final ColorRGBA TORCH_LIGHT_COLOR = new ColorRGBA(1f, .8f, 0.4f, 0);
    public static final float TORCH_LIGHT_RADIUS = 25f;
    SimpleWaterProcessor waterProcessor;
    public static final AmbientLight ambLight = new AmbientLight();
    private Node sceneModel; // Node for the labyrinth sections
    private World world;
    private BulletAppState bulletAppState;
    private RigidBodyControl landscape;
    private Player player;
    private Vector3f startLoc;
    // Sound related nodes
    private AudioNode portalSound;
    private AudioNode waterdropSound;
    private AudioNode batSound;
    private Node scareNode;
    private float tScare = 0;
    private float tScareMax = 4f;

    public static void main(String[] args) {
        Main app = new Main();
        //app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        /**
         * Set up physics
         */
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        setDisplayStatView(false);

        //ulletAppState.getPhysicsSpace().enableDebug(assetManager);

        // We re-use the flyby camera for rotation, while positioning is handled by physics
        viewPort.setBackgroundColor(new ColorRGBA(0f, 0f, 0f, 1f));
        flyCam.setMoveSpeed(10f);
        setupLight(); // Pq convem q seja por iluminaçao em vez de teres paredes iluminadas automagicamente

        setupWater();


        // Init scare node and sounds
        scareNode = new Node();

        // Load assets
        sceneModel = new Node("Labyrinth");
        WorldBlock.load(assetManager, waterProcessor);

        // Create world
        world = new World(sceneModel);
        world.generateWorld();


        // Need to be here so that the world is already created
        setupSound();

        // Attach the portal effect quads to be drawn
        rootNode.attachChild(world.getNonCollidables());

        // Setup the Player
        setupHoracio(); // Needs to be called after startLoc is initialized        


        // We set up collision detection for the scene by creating a
        // compound collision shape and a static RigidBodyControl with mass zero.
        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        // We attach the scene and the player to the rootnode and the physics space,
        // to make them appear in the game world.
        rootNode.attachChild(sceneModel);

        toonify(player);
        bulletAppState.getPhysicsSpace().add(landscape);

    }

    /**
     * Setup the player
     */
    private void setupHoracio() {

        startLoc = world.getStartPos();

        // TODO Change this thingy
        Spatial s_horacio = assetManager.loadModel("Models/horacio/Cube.mesh.j3o");
        s_horacio.setLocalScale(.6f);
        s_horacio.setLocalTranslation(0f, -1.2f, 0f);

        Quaternion roll180 = new Quaternion();
        roll180.fromAngleAxis(FastMath.PI, new Vector3f(0, 1, 0));
        s_horacio.setLocalRotation(roll180);

        // SET UP HORACIO'S TORCH
        Spatial torchLegit = assetManager.loadModel("Models/torch/torch.mesh.j3o");



        Material materialTorch = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        materialTorch.setTexture("Texture", assetManager.loadTexture("Effects/Explosion/flame.png"));
        ParticleEmitter fireEffect = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
        fireEffect.setMaterial(materialTorch);
        fireEffect.setImagesX(2);
        fireEffect.setImagesY(2); // 2x2 texture animation
        fireEffect.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
        fireEffect.setStartColor(new ColorRGBA(1f, 1f, 0.4f, 0.5f)); // yellow
        fireEffect.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fireEffect.setStartSize(0.3f);
        fireEffect.setEndSize(0.05f);
        fireEffect.setGravity(0f, 0f, 0f);
        fireEffect.setLowLife(0.5f);
        fireEffect.setHighLife(1f);
        fireEffect.getParticleInfluencer().setVelocityVariation(0.1f);
        fireEffect.setShape(new EmitterSphereShape(Vector3f.ZERO, .0666f));
        fireEffect.setQueueBucket(Bucket.Translucent);
        Node torch = new Node();
        torchLegit.getControl(SkeletonControl.class).getAttachmentsNode("Bone.001").attachChild(fireEffect);
        torchLegit.rotate(FastMath.DEG_TO_RAD * 55, 0, 0);
        torchLegit.move(0, .2f, -.2f);
        torchLegit.scale(0.4f);
        torch.attachChild(torchLegit);

//        AudioNode torchSound = ((AudioNode)torch.getChild("torchfire"));
//        torchSound.setVolume(3f);
//        torchSound.play();


        // Torch light setup
        PointLight lamp_light = new PointLight();
        lamp_light.setColor(TORCH_LIGHT_COLOR.mult(1f)); //ColorRGBA.Orange.mult(1.1f)
        lamp_light.setRadius(TORCH_LIGHT_RADIUS);
        rootNode.addLight(lamp_light);
        LightControl lc = new LightControl(lamp_light);
        lc.setControlDir(LightControl.ControlDirection.SpatialToLight);

        fireEffect.addControl(lc);

        final int SHADOWMAP_SIZE = 1024;
        PointLightShadowRenderer plsr = new PointLightShadowRenderer(assetManager, SHADOWMAP_SIZE);
        plsr.setLight(lamp_light);
        viewPort.addProcessor(plsr);

        player = new Player(s_horacio, inputManager, cam, torch);
        player.getCharacterControl().setPhysicsLocation(startLoc);

        rootNode.attachChild(player);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        CartoonEdgeFilter toonFilter = new CartoonEdgeFilter();
        FogFilter fogFilter = new FogFilter();
        fogFilter.setFogDistance(25 * 4);
        fogFilter.setFogColor(new ColorRGBA(0f, 0f, 0f, 1.0f));
        fogFilter.setFogDensity(1.5f);
        fpp.addFilter(fogFilter);
        toonFilter.setEdgeIntensity(1.5f);
        toonFilter.setEdgeWidth(2);
        fpp.addFilter(toonFilter);
        viewPort.addProcessor(fpp);
        viewPort.addProcessor(waterProcessor);



        bulletAppState.getPhysicsSpace().add(player.getCharacterControl());
    }

    private void jumpScareGenerator(float tpf) {
        tScare += tpf;
        if (tScare > tScareMax) {
            // Pick one of the sounds
            
            // Setup random sound location
            Vector3f pos = new Vector3f(FastMath.nextRandomFloat(), 0, FastMath.nextRandomFloat());
            pos = pos.mult(FastMath.nextRandomInt(10, 20));
            pos = pos.add(player.getWorldTranslation());

            // Maybe change it so direction is also random
            //Vector3f dir = pos.subtract(player.getWorldTranslation());
            Vector3f dir = new Vector3f(FastMath.nextRandomFloat(), 0, FastMath.nextRandomFloat());
            
            // Setup sound Node
            scareNode.setLocalTranslation(pos);
  
            if(FastMath.nextRandomFloat()< .8){
                waterdropSound.setDirection(dir);
                waterdropSound.setPitch(FastMath.nextRandomFloat() * 0.2f + 0.9f);
                LowPassFilter lpf = new LowPassFilter(FastMath.nextRandomFloat(), FastMath.nextRandomFloat());
                waterdropSound.setDryFilter(lpf);
                waterdropSound.playInstance();
            } else {
                batSound.setDirection(dir);
                batSound.setPitch(FastMath.nextRandomFloat() * 0.2f + 0.9f);
                LowPassFilter lpf = new LowPassFilter(FastMath.nextRandomFloat(), FastMath.nextRandomFloat());
                batSound.setDryFilter(lpf);
                batSound.playInstance();
            }
            
            
            tScare = 0;
            tScareMax = FastMath.nextRandomInt(3, 6);
        }

        // setup random pitch and whatnot


        // pick random sound


//        System.out.println("Translation:" + pos);
//        System.out.println("Player" + player.getWorldTranslation());
    }

    private void setupSound() {

        Node sTest = new Node();
        //sTest.setLocalTranslation(world.getStartPos().add(5,-3,5));
        sTest.setLocalTranslation(world.endPos);

        portalSound = new AudioNode(assetManager, "Sounds/portalwe.ogg");
//        Box marker = new Box(Vector3f.ZERO, 2, 1, 1);
//        Geometry markGeo = new Geometry("t", marker);
//        Material markMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        markGeo.setMaterial(markMat);
//        sTest.attachChild(markGeo);
        
        // Set geometry
        portalSound.setLooping(true);
        portalSound.setPositional(true);
        portalSound.setDirectional(true);
        
        portalSound.setReverbEnabled(false);
        portalSound.setVolume(50f);
        portalSound.setRefDistance(0.1f);

        portalSound.setDirection(new Vector3f(-1, 0, 0));
        portalSound.setInnerAngle(50);
        portalSound.setOuterAngle(360); 



        portalSound.setMaxDistance(10000f);
        //portalSound.setLocalTranslation(sTest.getLocalTranslation());
        sTest.attachChild(portalSound);


        rootNode.attachChild(sTest);
     //   markMat.setColor("Color", ColorRGBA.Blue);


        portalSound.play();


        // Setup scary sounds
        waterdropSound = new AudioNode(assetManager, "Sounds/waterdrop.ogg");
        batSound = new AudioNode(assetManager, "Sounds/bats.ogg");
        waterdropSound.setPositional(true);
        batSound.setPositional(true);
        waterdropSound.setVolume(3f);
        batSound.setVolume(3f);
        waterdropSound.setDirectional(true);
        batSound.setReverbEnabled(false);
        waterdropSound.setReverbEnabled(false);
        scareNode.attachChild(waterdropSound);
        scareNode.attachChild(batSound);


        Environment env = Environment.Garage;
        audioRenderer.setEnvironment(env);

    }

    private void setupLight() {



        // We add light so we see the scene
        ambLight.setColor(ColorRGBA.White.mult(.5f));
        rootNode.addLight(ambLight);

        // You must add a light to make the model visible TODO add torches everywhere
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
        sun.setColor(ColorRGBA.White.mult(0.1f));
        rootNode.addLight(sun);

//         final int SHADOWMAP_SIZE=1024;
//        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 2);
//        dlsr.setLight(sun);
//        viewPort.addProcessor(dlsr);


        // Set up Fog



        rootNode.setShadowMode(ShadowMode.Off);

    }

    /**
     * These are our custom actions triggered by key presses. We do not walk
     * yet, we just keep track of the direction the user pressed.
     */
    public void onAction(String binding, boolean isPressed, float tpf) {
    }

    /**
     * Setup water processor
     */
    public void setupWater() {
        waterProcessor = new SimpleWaterProcessor(assetManager);
        waterProcessor.setDistortionScale(1f);
        waterProcessor.setWaterDepth(0);
        waterProcessor.setWaterColor(ColorRGBA.Blue.mult(50));
        waterProcessor.setReflectionScene(rootNode);


    }

    /**
     * This is the main event loop--walking happens here. We check in which
     * direction the player is walking by interpreting the camera direction
     * forward (camDir) and to the side (camLeft). The setWalkDirection()
     * command is what lets a physics-controlled player walk. We also make sure
     * here that the camera moves with player.
     */
    @Override
    public void simpleUpdate(float tpf) {
        player.update(tpf, sceneModel);

        // Update the listener
        listener.setLocation(player.getWorldTranslation());
        listener.setRotation(player.getWorldRotation().opposite());


        jumpScareGenerator(tpf);

//        System.out.println("PortalSound Pos:" + portalSound.getWorldTranslation());
//          System.out.println("Portal Pos:" + world.endPos);


        // Complete falloff depending on distance
        if (player.getWorldTranslation().distance(portalSound.getWorldTranslation()) > 50) {
            portalSound.pause();
        } else {
            portalSound.play();
        }


        if (world.getBlockTypeAt(player.getLocalTranslation()) == WorldGen.END) {
            reroll();
        }
    }

    public void toonify(Spatial spatial) {
        if (spatial instanceof Node) {
            Node n = (Node) spatial;
            for (Spatial child : n.getChildren()) {
                toonify(child);
            }
        } else if (spatial instanceof Geometry) {
            Geometry g = (Geometry) spatial;
            Material m = g.getMaterial();
            if (m.getMaterialDef().getName().equals("Phong Lighting")) {
                Texture t = assetManager.loadTexture(
                        "Textures/ColorRamp/toon.png");
                m.setTexture("ColorRamp", t);

                m.setBoolean("VertexLighting", true);
                m.setBoolean("UseMaterialColors", true);
            }
        }
    }
//    @Override
//    public void simpleRender(RenderManager rm) {
//        //TODO: add render code
//    }

    /**
     * Cleans the current state and then generates a new level and places the
     * player on it
     */
    private void reroll() {
        // Remove previous scene from rootNode and physics space
        rootNode.detachChild(sceneModel);
        bulletAppState.getPhysicsSpace().remove(landscape);

        // Create new Scene and World and attach them
        sceneModel = new Node("Labyrinth");
        world = new World(sceneModel);
        world.generateWorld();

        rootNode.attachChild(world.getNonCollidables());

        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
        landscape = new RigidBodyControl(sceneShape, 0);
        sceneModel.addControl(landscape);

        rootNode.attachChild(sceneModel);
        bulletAppState.getPhysicsSpace().add(landscape);

        setupSound();

        // Position player
        player.getCharacterControl().setPhysicsLocation(world.getStartPos());
    }
}
