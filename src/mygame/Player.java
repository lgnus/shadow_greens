/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.animation.SkeletonControl;
import com.jme3.audio.AudioNode;
import com.jme3.audio.LowPassFilter;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.light.PointLight;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LightControl;
import java.util.Random;

public class Player extends Node implements ActionListener, AnalogListener, AnimEventListener {
    
    private Camera cam;
    private ThirdPersonCamera camera3P;
    private boolean toggleCheat = false;
    
    private Spatial model;
    private InputManager inputManager;
    private Vector3f walkDirection = new Vector3f();
    private CharacterControl control;
    
    private AnimChannel animChannel;
    private AnimControl animControl;
    
    // Animation names
    private String idleAnim = "idle";
    private String walkAnim = "walk";
         
    
    private int viewDistance = 6;
    private float timeCounter = 0f;
    
    // Sound variables
    private LowPassFilter lpf;
    private float tSound = 0;
    private float tPause = .6f;
    
    private Node torch;
    private Random rng = new Random();
    PointLight torchLight;
    
    // Options: tinker around
    private float walkSpeed = .15f;
    private float mouselookSpeed = FastMath.PI;
    private float jumpSpeed = 6;
    private float fallSpeed = 9.8f;
    private float gravity = 9.8f;
    private float stepSize = .05f;
    
    private boolean left = false;
    private boolean right = false;
    private boolean up = false;
    private boolean down = false;
    
    private AudioNode footstepSound;
    private AudioNode backgroundSound;
    private AudioNode torchSound;
    
    
    //Temporary vectors used on each frame.
    //They're here to avoid instanciating new vectors on each frame
    private Vector3f camDir = new Vector3f();
    private Vector3f camLeft = new Vector3f();
    
    public Player(Spatial model, InputManager inputManager, Camera cam){
        super();
        
        
        
        this.cam = cam;
        camera3P = new ThirdPersonCamera("CamNode", cam, this);
        
        cam.setFrustumFar(viewDistance * WorldBlock.SIZE);
        
        // Adds this model to the player node
        this.model = model;
        model.setShadowMode(ShadowMode.Cast);
       // model.setCullHint(CullHint.Never);
        this.attachChild(model);
        
        
      
        
        
        
        // We set up collision detection for the player by creating
        // a capsule collision shape and a CharacterControl.
        // The CharacterControl offers extra settings for
        // size, stepheight, jumping, falling, and gravity.
        // We also put the player in its starting position.
        CapsuleCollisionShape playerShape = new CapsuleCollisionShape(.75f, 1f);
	control = new CharacterControl(playerShape, stepSize);
	control.setJumpSpeed(jumpSpeed);
	control.setFallSpeed(fallSpeed);
	control.setGravity(gravity);
        this.addControl(control);
        
        // Animation setup
        animControl = model.getControl(AnimControl.class);
        animControl.addListener(this);
        animChannel = animControl.createChannel();
        animChannel.setAnim(idleAnim);
        
        // Setup the key bindings
        this.inputManager = inputManager;
        setupKeys();
    }
    
    public Player(Spatial model, InputManager inputManager, Camera cam, Node torch){
        this(model, inputManager, cam);
        this.attachChild(torch);
        
        
        // setup footstep sound
        footstepSound = ((AudioNode)this.getChild("footsteps"));
        backgroundSound = ((AudioNode)this.getChild("background"));
        torchSound = ((AudioNode)torch.getChild("torchfire"));
        torchSound.setVolume(.2f);
        backgroundSound.setVolume(.1f);
        backgroundSound.play();
        torchSound.play();
      
        
        
        this.torch = torch;
        torch.setShadowMode(ShadowMode.Off);
        model.getControl(SkeletonControl.class).getAttachmentsNode("Bone.008").attachChild(torch);
        // you could have prevented this...
        torchLight = (PointLight) torch.getChild(0).getControl(SkeletonControl.class).getAttachmentsNode("Bone.001").getChild("Emitter")/*but you didn't*/.getControl(LightControl.class).getLight(); /*and now you pay*/
    
    
    }
    
    public void onAction(String binding, boolean isPressed, float tpf){
      if (binding.equals("Left")) {
        left = isPressed;
      } else if (binding.equals("Right")) {
        right= isPressed;
      } else if (binding.equals("Up")) {
        up = isPressed;
      } else if (binding.equals("Down")) {
        down = isPressed;
      } else if (binding.equals("Jump")) {
        if (isPressed) { 
            control.jump();
        }
      } else if (binding.equals("Cheat")){
          if(toggleCheat){
              // TODO, go to the end
          }else {
             
          }
          
          toggleCheat = !toggleCheat;
      }
    }
    
    public void onAnalog(String binding, float value, float tpf) {
        if (binding.equals("TurnLeft")) {
            Quaternion turn = new Quaternion();
            turn.fromAngleAxis(mouselookSpeed*value, Vector3f.UNIT_Y);
            control.setViewDirection(turn.mult(control.getViewDirection()));
        }
        else if (binding.equals("TurnRight")) {
            Quaternion turn = new Quaternion();
            turn.fromAngleAxis(-mouselookSpeed*value, Vector3f.UNIT_Y);
            control.setViewDirection(turn.mult(control.getViewDirection()));
        }
        else if (binding.equals("MouseLookDown")) {
            camera3P.verticalRotate(mouselookSpeed*value);
        }
        else if (binding.equals("MouseLookUp")) {
            camera3P.verticalRotate(-mouselookSpeed*value);
        }
    }
    
    
    public void update(float tpf, Node sceneModel) {
        
        
        camDir.set(cam.getDirection());
        camDir.y = 0f;
        camLeft.set(cam.getLeft());
        camLeft.y = 0f;
        walkDirection.set(0, 0, 0);
        
        if (left) {
            walkDirection.addLocal(camLeft); 
            
        }
        if (right){ 
            walkDirection.addLocal(camLeft.negate());
            
        }
        if (up)   { 
            walkDirection.addLocal(camDir);
            
        }
        if (down) {
            walkDirection.addLocal(camDir.negate()); 
            
        }
        
        // Physics-based movement
        control.setWalkDirection(walkDirection.normalize().multLocal(walkSpeed));
        // First-person camera
        //cam.setLocation(control.getPhysicsLocation());
        camera3P.update(sceneModel);
        
        handleAnimations();
        handleSound(tpf);
        
        // Torch light flickering
        //float factor = 1f + (0.2f*rng.nextFloat() - 0.1f);
        float c_factor = (float) (1f + (0.015f*rng.nextGaussian()));
        float r_factor = (float) (1f + (0.07f*rng.nextGaussian()));
        torchLight.setColor(Main.TORCH_LIGHT_COLOR.mult(c_factor));
        torchLight.setRadius(Main.TORCH_LIGHT_RADIUS * r_factor);
        
        // Torch motion bobbing
        timeCounter += tpf;
        
        if (timeCounter < 1f) {
            torch.move(0, -tpf/7, 0);
        } else if (timeCounter < 2f) {
            torch.move(0, tpf/7, 0);
        } else {
            timeCounter = 0;
        }
    }
    
    /** Handles animations, what did you expect?
     */
    private void handleAnimations() {
        if (control.onGround())
        { // If player is on the ground
            if (up || left || right || down)
            { // and player is moving
                          
                
                if (!animChannel.getAnimationName().equals(walkAnim))
                { // and it's not already playing the walk animation
                   
                    animChannel.setAnim(walkAnim, 0.3f);
                    animChannel.setSpeed(2f);
                    animChannel.setLoopMode(LoopMode.Loop);
                }
            }
            else
            { // and player is not moving
                if (!animChannel.getAnimationName().equals(idleAnim))
                { // and it's not already playing the idle animation
                    animChannel.setAnim(idleAnim, 0.3f);
                    animChannel.setSpeed(.3f);
                    animChannel.setLoopMode(LoopMode.Cycle);
                }
            } // movement check else clause
        } // onGround check
    }
    
    private void handleSound(float tpf){
        
        
        // Change torch slightly
        torchSound.setPitch(FastMath.nextRandomFloat()*0.2f+0.9f);
        
   
        
  
        if((up || down ||left||right)&& control.onGround()){
            if(tSound  > tPause){
                footstepSound.setPitch(FastMath.nextRandomFloat()*0.2f+0.9f);
               
                
                lpf = new LowPassFilter(FastMath.nextRandomFloat(),FastMath.nextRandomFloat());
                
                footstepSound.setDryFilter(lpf);
                footstepSound.playInstance();
                tSound = 0;
            }
            tSound += tpf;
        }
        
    }
    
    public void onAnimCycleDone(AnimControl anCtrl, AnimChannel anChnl, String animName) {
        
    }
    
    public void onAnimChange(AnimControl anCtrl, AnimChannel anChnl, String animName) {
        
    }
    
    /** We over-write some navigational key mappings here, so we can
     * add physics-controlled walking and jumping: */
    private void setupKeys() {
      inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
      inputManager.addMapping("Cheat", new KeyTrigger(KeyInput.KEY_TAB));
      inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
      inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
      inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
      inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
      inputManager.addMapping("TurnLeft", new MouseAxisTrigger(MouseInput.AXIS_X,true));
      inputManager.addMapping("TurnRight", new MouseAxisTrigger(MouseInput.AXIS_X,false));
      inputManager.addMapping("MouseLookDown", new MouseAxisTrigger(MouseInput.AXIS_Y,true));
      inputManager.addMapping("MouseLookUp", new MouseAxisTrigger(MouseInput.AXIS_Y,false));
      inputManager.addListener(this, "Cheat");
      inputManager.addListener(this, "Left");
      inputManager.addListener(this, "Right");
      inputManager.addListener(this, "Up");
      inputManager.addListener(this, "Down");
      inputManager.addListener(this, "Jump");
      inputManager.addListener(this, "TurnLeft");
      inputManager.addListener(this, "TurnRight");
      inputManager.addListener(this, "MouseLookDown");
      inputManager.addListener(this, "MouseLookUp");
    }
    
    public CharacterControl getCharacterControl() {
        return control;
    }    
}
