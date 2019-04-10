/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import javax.vecmath.Point2i;

/**
 *
 * @author lgnus
 */
public class WorldGenMazeVersion {
    
    private HashMap<Character, Integer> dir = new HashMap<Character, Integer>(); 
    private HashMap<Character, Character> inv = new HashMap<Character, Character>();
    private HashMap<Character, Point2i> nav = new HashMap<Character, Point2i>(); 
    private Random rng = new Random();
    private boolean[][][] world;
    private int width, height;
    private Character[] directions = new Character[]{'U','D','L','R'};


    public static void main(String[] args){
	WorldGenMazeVersion n = new WorldGenMazeVersion(15,15);
        System.out.println(n.toString());
    }
    
    public WorldGenMazeVersion(int width, int height){
        this.width = width;
        this.height = height;
        
        world = new boolean[width][height][5];
        // Put directions and it's inverses
        dir.put('U', 0); dir.put('D', 1); dir.put('L', 2); dir.put('R', 3); 
        dir.put('V',4);
        inv.put('U','D');inv.put('D','U');inv.put('L','R');inv.put('R','L');
        nav.put('U', new Point2i(0,1));
        nav.put('D', new Point2i(0,-1));
        nav.put('R', new Point2i(1,0));
        nav.put('L', new Point2i(-1,0));
        
        int ix = rng.nextInt(width);
        int iy = rng.nextInt(height);
        System.out.println("Starting Position: (" + ix + ',' + iy + ")\n===========");
        recursiveBacktracking(ix,iy);
    }
    
    private void recursiveBacktracking(int ix, int iy){
        // Shuffle directions
        Collections.shuffle(Arrays.asList(directions));
        int nx,ny;
        for(Character c: directions){
            nx = ix + nav.get(c).x; 
            ny = iy + nav.get(c).y;
            
            if(allowed(nx, ny)){
                world[ix][iy][dir.get(c)] = true;
                world[ix][iy][dir.get('V')] = true;
                world[nx][ny][dir.get(inv.get(c))] = true;
                world[nx][ny][dir.get('V')] = true;
                recursiveBacktracking(nx, ny);
            }
            
        }
        
    }

    // Print the maze in ASCII
    public String toString(){
       String s = "";
       
       for(int i=height-1; i >= 0; i--){
           for(int j = 0; j<width; j++){
               if(world[j][i][dir.get('U')]){
                   s+='U';
               }
               if(world[j][i][dir.get('D')]){
                   s+='D';
               }
               if(world[j][i][dir.get('L')]){
                   s+='L';
               }
               if(world[j][i][dir.get('R')]){
                   s+='R';
               }
               //s+="x:"+j+"  y:"+i;
               s+='|';
           }
           s+='\n';
       }
       
       return s;
    }
    
    // Returns true if x and y are between boundaries and were not visited
    // false otherwise
    private boolean allowed(int x, int y){
        if((x >= 0) && (x < width) && (y >= 0) && (y < height)
                && !world[x][y][dir.get('V')]){
            return true;
        }  
        return false;
    }
        
}
