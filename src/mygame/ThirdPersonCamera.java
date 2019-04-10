package mygame;

import com.jme3.collision.CollisionResults;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl;

public class ThirdPersonCamera {
    
    private Node pivot;
    private CameraNode camNode;
    
    //private float chaseDistance = 7f;
    private float verticalAngle = 30 * FastMath.PI;
    
    private float maxVerticalAngle = 89 * FastMath.DEG_TO_RAD;
    private float minVerticalAngle = 1 * FastMath.DEG_TO_RAD;
    
    public ThirdPersonCamera(String name, Camera cam, Node player) {
        pivot = new Node("CamTrack");
        player.attachChild(pivot);
        
        camNode = new CameraNode(name, cam);
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        
        
        pivot.attachChild(camNode);
        camNode.setLocalTranslation(new Vector3f(0, 0, 7f)); // 0,0,chaseDistance
        camNode.lookAt(pivot.getLocalTranslation(), Vector3f.UNIT_Y);
        
        pivot.getLocalRotation().fromAngleAxis(-verticalAngle, Vector3f.UNIT_X);
    }
    
    
    public void verticalRotate(float angle) {
        verticalAngle += angle;
        
        if (verticalAngle < minVerticalAngle)
            verticalAngle = minVerticalAngle;
        else if (verticalAngle > maxVerticalAngle)
            verticalAngle = maxVerticalAngle;
        
        pivot.getLocalRotation().fromAngleAxis(-verticalAngle, Vector3f.UNIT_X);
    }
    
    public CameraNode getCameraNode() {
        return camNode;
    }
    
    public Node getCameraTrack() {
        return pivot;
    }
    
    public void update(Node sceneModel) {
        Ray ray = new Ray(pivot.getParent().getLocalTranslation(), 
                camNode.getCamera().getDirection().negate().normalize());
        CollisionResults result = new CollisionResults();
        sceneModel.collideWith(ray, result);
        
        float chaseDistance = 7f;
        if (result.getClosestCollision() != null)
            chaseDistance = Math.min(7f, result.getClosestCollision().getDistance());
        
        camNode.setLocalTranslation(new Vector3f(0, 0, chaseDistance));
    }
}
