/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ctf.agent;

import ctf.common.AgentAction;
import ctf.common.AgentEnvironment;
import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 *
 * @author evan
 */
public class ecr110030Agent extends Agent {
    
    //----Static class variables used to communicate between agents----//
    //Must be reset between every run since each match only has 1 round
    private static enum Direction { NORTH, SOUTH, EAST, WEST; }
    private static enum Entity { TEAMMATE, ENEMY, OBSTACLE, EMPTY, OURFLAG, ENEMYFLAG, OURBASE, ENEMYBASE; }
    private static boolean staticVariablesReset = false;    
    private static int agentNumInitializer = 0;
    //private static int maxAgentNum = 0;
    private static Direction baseSide = Direction.EAST;
    private static Point base;
    private static boolean determinedBaseSide = false;
    private static int mapSize = 10;
    private static boolean determinedMapSize = false;
    private static Point enemyBase;
    private static boolean foundEnemyBase = false;
    private static HashMap<Point, Entity> map = new HashMap<>();
    
    //Any static variables that must be reset (almost all) should be re-assigned here
    private void resetStaticVariables() { staticVariablesReset = true;
        //maxAgentNum = agentNumInitializer;
        agentNumInitializer = 0;    //gets reset after agents have been constructed for a team
        baseSide = Direction.EAST;
        determinedBaseSide = false;
        mapSize = 10;
        determinedMapSize = false;
        enemyBase = new Point(0,4);
        foundEnemyBase = false;
        map = new HashMap<>();
    }
    
    //----Private local Agent variables and constructor----//
    private final int agentNum;
    private int turnNum;
    private Point location;
    private Entity previousEntity;
    private Point spawnLocation;
    AgentEnvironment inEnvironment; //set every time getMove is called
    
    //must only use no-arg constructor
    public ecr110030Agent(){
        agentNum = agentNumInitializer++;
        turnNum = 0;
        location = new Point();
        //initialize localMap
        /*
        for(int y=0; y<mapSize; y++)
            for(int x=0; x<mapSize; x++)
                localMap.put(new Point(x,y), Entity.EMPTY);
        
        */
    }
    
    //----Private local variables used while still determining board state----//
    //Do not need to be reset each run, as default values are initialized each run
    private boolean firstMove = true;
    private Direction startSide = Direction.NORTH;
    private boolean moveNorth = false;
    private int startingColumnSize = 1; //Agent checks next square and counts himself
    //Variables for locally storing map data
    private Point localStartingLocation = new Point();
    private HashMap<Point, Entity> localMap = new HashMap<>();
    private boolean migrateData = true;
    //------------------------------------------------------------------------//
    
    
    //determines which side of base agents are on and decides if agent needs to calculate map size
    private void firstMoveAndSouthSideCounts() {
        if(firstMove) { firstMove = false;
            startSide = inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM, false) ? Direction.SOUTH : Direction.NORTH;
            moveNorth = (startSide==Direction.SOUTH && !inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM,true));
            if(!staticVariablesReset) resetStaticVariables(); //only works for 2 agents
            //localMap.put(localStartingLocation, Entity.TEAMMATE);
        } else if(staticVariablesReset) staticVariablesReset = false;
    }
    
    
    
    /**
     * Calculates the next move for an agent.
     * @param inEnvironment the environment in which the agent is in
     * @return action which to take
     */
    @Override
    public int getMove(AgentEnvironment inEnvironment) { 
        this.inEnvironment = inEnvironment; turnNum++; System.out.println();
        this.firstMoveAndSouthSideCounts();                                           
        if(!determinedBaseSide) baseSide = inEnvironment.isBaseWest(AgentEnvironment.ENEMY_TEAM, false) ? Direction.EAST : Direction.WEST;//
        //initialization of map size and grid
        if(moveNorth){ //calculates map size, assuming map size is even integer.
            //printShit();
            startingColumnSize++;
            if(!inEnvironment.isFlagNorth(AgentEnvironment.OUR_TEAM, true)) {
                this.scanAround(localMap);
                return this.localMove(Direction.NORTH); }
            else {
                moveNorth = false;
                determinedMapSize = true;
                mapSize = 2*(startingColumnSize/*+(maxAgentNum/2 - 1)*/);
                //initialize map
                for(int j=0; j<mapSize; j++) {
                    for(int k=0; k<mapSize; k++) {
                        map.put(new Point(j,k), Entity.EMPTY);}
                //location = new Point(baseSide==Direction.EAST ? 0 : -mapSize+1, (mapSize-1)/2 - 1); 
                base = new Point(baseSide==Direction.EAST ? mapSize-1 : 0,(mapSize-1)/2);
                //base.translate(baseSide==Direction.EAST ? mapSize-1 : 0, 1);
                enemyBase = new Point(baseSide==Direction.EAST ? 0 : mapSize-1,(mapSize-1)/2);
                //goal.translate(0,1); 
                } }
        } 
        //----Both agent actions----------------------------------------------//
        //locally store data until map size has been determined
        if(!determinedMapSize) {
            return AgentAction.DO_NOTHING;
            /* //old (broken?) code
            this.scanAround(localMap);  //scan around current location
            Point p = new Point(localStartingLocation);
            p.translate(-1, 0);
            if(localMap.get(p)==Entity.EMPTY)
                return this.localMove(Direction.WEST);
            else {
                p = new Point(localStartingLocation);
                p.translate(0, -1);
                if(localMap.get(p)==Entity.EMPTY)
                    return this.localMove(Direction.SOUTH);
            }
            */
            //return this.localMove();//make a local move and update old/new locations
        }
        //now, migrate agent data to shared map
        if(determinedMapSize) {
            //migrate data from localMap to map
            if(migrateData) { migrateData = false;
                //reminder: agents are initialzed from top to bottom 
                //spawnLocation = new Point(baseSide==Direction.WEST ? mapSize-1 : 0, agentNum <= (maxAgentNum-1)/2 ? mapSize-1-agentNum : 0+maxAgentNum-1-agentNum);
                //only works for 2 agents
                spawnLocation = new Point(baseSide==Direction.WEST ? 0 : mapSize-1, agentNum%2==0 ? mapSize-1 : 0);
                localMap.keySet().forEach((Point p) -> {
                    Point newP = new Point(p);
                    newP.translate(spawnLocation.x, spawnLocation.y);
                    //only add non-empty squares
                    if(localMap.get(p)!=Entity.EMPTY)
                        map.put(newP,localMap.get(p));
                });
                Point newP = new Point(location);
                newP.translate(spawnLocation.x, spawnLocation.y);
                this.location = newP;
            }
            //System.out.println("(" + this.location.x + ',' + this.location.y + ')');
            printInfo();
            
            
            /***********************\
************** AI logic goes here. **************
            \***********************/
            //Check to see if position was reset
            if(!inEnvironment.isBaseEast(AgentEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseWest(AgentEnvironment.OUR_TEAM,false)) {
                if(startSide==Direction.SOUTH && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM,false)){
                    resetPosition();
                }
                else if(startSide==Direction.NORTH && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isBaseSouth(AgentEnvironment.OUR_TEAM,false)){
                    resetPosition();
                }
            }
                
            
            //scan around agent
            this.scanAround(map);
            
            //if enemy does not have flag
            if(!inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM))
            {
                //if we do not have the enemy flag
                if(!inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM))
                {
                    //head to enemy base
                    return this.move(astar(map,enemyBase));
                }
                //else if we do have the enemy flag
                else 
                {
                    //if our team has the flag but it is not you
                    if(inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM) && !inEnvironment.hasFlag())
                    {
                        //head to enemy base
                        return this.move(astar(map,enemyBase));
                    }
                    //else if our team has the flag and you are that agent
                    else if(inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM) && inEnvironment.hasFlag())
                    {
                        //head back to base
                        return this.move(astar(map,base));
                    }
                } 
                //else if a teammate has flag and you are that teammate
            } 
            //else if enemy has our flag
            else 
            {
                //if we do not have the enemy flag
                if(!inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM))
                {
                    //head to enemy base
                    return this.move(astar(map,enemyBase));
                }
                //else if we do have the enemy flag
                else 
                {
                    //if teammate has flag and you do not
                    if(inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM) && !inEnvironment.hasFlag())
                    {
                        //head to enemy base
                        return this.move(astar(map,enemyBase));
                    } 
                    //else if a teammate has flag and you are that teammate
                    else if(inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM) && inEnvironment.hasFlag())
                    {
                        //head back to base
                        return this.move(astar(map,base));
                    }
                }
            }  
        }
        //else, if map size has not yet been determined:
        
        //do nothing for now once map initialized...
        return AgentAction.DO_NOTHING;
    }
    
    
    //----Methods for agent movement and updating map(s)----------------------//
    private int move(Direction direction)
    {
        map.put(new Point(location), Entity.EMPTY);
        Point newLocation = new Point(location);
        if(direction==null) return AgentAction.DO_NOTHING;
        switch(direction) {
            case NORTH: newLocation.translate( 0, 1); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; printMap();  return AgentAction.MOVE_NORTH;
            case SOUTH: newLocation.translate( 0,-1); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; printMap();  return AgentAction.MOVE_SOUTH;
            case EAST:  newLocation.translate( 1, 0); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; printMap();  return AgentAction.MOVE_EAST;
            case WEST:  newLocation.translate(-1, 0); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; printMap();  return AgentAction.MOVE_WEST;
            default:    map.put(new Point(location), Entity.ENEMY); return AgentAction.DO_NOTHING;
        }
    }
    
    //seems to work?
    private void resetPosition() 
    {
        map.put(new Point(location), Entity.EMPTY);
        this.location = this.spawnLocation;
        map.put(new Point(location), Entity.TEAMMATE);
    }
    
    private int localMove(Direction direction)
    {
        localMap.put(new Point(location), Entity.EMPTY);
        Point newLocation = new Point(location);
        switch(direction) {
            case NORTH: newLocation.translate( 0, 1); localMap.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_NORTH;
            case SOUTH: newLocation.translate( 0,-1); localMap.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_SOUTH;
            case EAST:  newLocation.translate( 1, 0); localMap.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_EAST;
            case WEST:  newLocation.translate(-1, 0); localMap.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_WEST;
            default:    localMap.put(new Point(location), Entity.ENEMY); return AgentAction.DO_NOTHING;
        }
    }
    //------------------------------------------------------------------------//
    
    
    //----Methods for scanning around agent-----------------------------------//
    private void scanAround(HashMap<Point, Entity> map){ 
        for(Direction dir : Direction.values())
        {
            Point p = new Point(location);
            Entity e = Entity.EMPTY;
            switch(dir) {
                case NORTH: p.translate( 0, 1); e = scanAdjacent(Direction.NORTH); break;
                case SOUTH: p.translate( 0,-1); e = scanAdjacent(Direction.SOUTH); break;
                case EAST:  p.translate( 1, 0); e = scanAdjacent(Direction.EAST);  break;
                case WEST:  p.translate(-1, 0); e = scanAdjacent(Direction.WEST);  break;
            }
            map.put(p, e);
        }
        
    }
    //scans in a direction from the agent
    private Entity scanAdjacent(Direction direction){
        Entity tile = Entity.EMPTY;
        switch(direction) {
            case NORTH: tile = inEnvironment.isObstacleNorthImmediate()                 ? Entity.OBSTACLE 
                        : inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM,true)    ? Entity.TEAMMATE
                        : inEnvironment.isAgentNorth(AgentEnvironment.ENEMY_TEAM,true)  ? Entity.ENEMY
                        : inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM,true)     ? Entity.OURBASE
                        : inEnvironment.isBaseNorth(AgentEnvironment.ENEMY_TEAM,true)   ? Entity.ENEMYBASE
                        : inEnvironment.isFlagNorth(AgentEnvironment.OUR_TEAM,true)     ? Entity.OURFLAG
                        : inEnvironment.isFlagNorth(AgentEnvironment.ENEMY_TEAM,true)   ? Entity.ENEMYFLAG
                        : Entity.EMPTY; break;
            case SOUTH: tile = inEnvironment.isObstacleSouthImmediate()                 ? Entity.OBSTACLE 
                        : inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM,true)    ? Entity.TEAMMATE
                        : inEnvironment.isAgentSouth(AgentEnvironment.ENEMY_TEAM,true)  ? Entity.ENEMY
                        : inEnvironment.isBaseSouth(AgentEnvironment.OUR_TEAM,true)     ? Entity.OURBASE
                        : inEnvironment.isBaseSouth(AgentEnvironment.ENEMY_TEAM,true)   ? Entity.ENEMYBASE
                        : inEnvironment.isFlagSouth(AgentEnvironment.OUR_TEAM,true)     ? Entity.OURFLAG
                        : inEnvironment.isFlagSouth(AgentEnvironment.ENEMY_TEAM,true)   ? Entity.ENEMYFLAG
                        : Entity.EMPTY; break;
            case EAST:  tile = inEnvironment.isObstacleEastImmediate()                  ? Entity.OBSTACLE 
                        : inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM,true)     ? Entity.TEAMMATE
                        : inEnvironment.isAgentEast(AgentEnvironment.ENEMY_TEAM,true)   ? Entity.ENEMY
                        : inEnvironment.isBaseEast(AgentEnvironment.OUR_TEAM,true)      ? Entity.OURBASE
                        : inEnvironment.isBaseEast(AgentEnvironment.ENEMY_TEAM,true)    ? Entity.ENEMYBASE
                        : inEnvironment.isFlagEast(AgentEnvironment.OUR_TEAM,true)      ? Entity.OURFLAG
                        : inEnvironment.isFlagEast(AgentEnvironment.ENEMY_TEAM,true)    ? Entity.ENEMYFLAG
                        : Entity.EMPTY; break;
            case WEST:  tile = inEnvironment.isObstacleWestImmediate()                  ? Entity.OBSTACLE 
                        : inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM,true)     ? Entity.TEAMMATE
                        : inEnvironment.isAgentWest(AgentEnvironment.ENEMY_TEAM,true)   ? Entity.ENEMY
                        : inEnvironment.isBaseWest(AgentEnvironment.OUR_TEAM,true)      ? Entity.OURBASE
                        : inEnvironment.isBaseWest(AgentEnvironment.ENEMY_TEAM,true)    ? Entity.ENEMYBASE
                        : inEnvironment.isFlagWest(AgentEnvironment.OUR_TEAM,true)      ? Entity.OURFLAG
                        : inEnvironment.isFlagWest(AgentEnvironment.ENEMY_TEAM,true)    ? Entity.ENEMYFLAG
                        : Entity.EMPTY; break;
        }
        return tile;
    } 
    //------------------------------------------------------------------------//
    
    
    //----Methods for determining where to move-------------------------------//
    private class Node{
        Point p = new Point();
        Point goal;
        Node parent;
        Direction moveDir; //direction taken to reach this node
        int f, g, h;
        
        private Node(Point thisPoint, Point goal) { p = thisPoint; this.goal = goal; g = 0; h = manhattanDistance(goal); f = g + h; }
        private Node(Point thisPoint, Point goal, Node parentNode, Direction dir) {
            p = thisPoint;
            this.goal = goal;
            parent = parentNode;
            moveDir = dir;
            g = this.parent.g+1;
            h = manhattanDistance(goal);
            f = this.g + this.h;
            //System.out.println("Point: ("+p.x+","+p.y+"); f="+f+", g="+g+", h="+h);
        }
        private int manhattanDistance(Point goal) {
            //System.out.println("Mh dist: ("+goal.c.x+","+goal.c.y+") - ("+this.p.x+","+this.p.y+") = "+(Math.abs(goal.c.x - this.p.x) + Math.abs(goal.c.y - this.p.y)));
            return Math.abs(goal.x - this.p.x) + Math.abs(goal.y - this.p.y); 
        }
        //Consider nodes equal if they are located at the same point; A* is run on a fixed snapshot of the graph, 
        //and we do not want cycles in our path (so the point (1,3) with f=4 and later (1,3) with f=6 is undesirable; only f=4 should be considered.
        @Override
        public boolean equals(Object o)
        {
            if(o.getClass() != this.getClass()) return true;
            return this.p.equals(((Node) o).p);
        }
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + Objects.hashCode(this.p);
            return hash;
        }
    }
    
    //A* path finding algorithm
    private Direction astar(HashMap<Point, Entity> map, Point goal)
    {
        if(location.equals(goal))
            return null;
        
        //initialize priority queue and associated set
        PriorityQueue<Node> searchGraph = new PriorityQueue<>(
                (Node n1, Node n2) -> { 
                    return n1.f == n2.f ? n1.h == n2.h ? n1.p.x - n2.p.x : n1.h - n2.h : n1.f - n2.f; 
        });
        
        searchGraph.add(new Node(location, goal));
        HashSet<Node> searchSet = new HashSet<>();
        searchSet.add(new Node(location, goal));
        System.out.println("Current: ("+location.x+","+location.y+")  Goal: ("+goal.x+","+goal.y+")  Enemy Base: ("+ecr110030Agent.enemyBase.x+","+ecr110030Agent.enemyBase.y+")  Base: ("+base.x+","+base.y+")");
        
        //goalLeaf = null if no path found, which will not happen since duplicates are allowed for now
        Node goalLeaf = successors(map, searchGraph, searchSet, goal);
        System.out.println("Successor: "+goalLeaf.p.toString());
        Direction moveDirection = goalLeaf.moveDir;
        while(true) { 
            if(goalLeaf.parent != null && goalLeaf.parent.moveDir == null)
                break;
            else {
                goalLeaf = goalLeaf.parent;
                moveDirection = goalLeaf.moveDir;
            }
        }
        return moveDirection;
    }
    
    private Node successors(HashMap<Point, Entity> map, PriorityQueue<Node> searchGraph, HashSet<Node> searchSet, Point goal)
    {
        if(searchGraph.isEmpty()) return null;
        else {
            Node n = searchGraph.poll();
            System.out.println("Selected: "+n.p.toString() + " "+n.f+"="+n.g+"+"+n.h);
                /* Check if this tile is the enemy flag tile; may need to be changed later */
            if(n.p.equals(goal)) return n;
            else {
                for(Direction dir : Direction.values())
                {
                    Point p = new Point(n.p);
                    switch(dir){
                        case NORTH: p.translate( 0, 1); break;
                        case SOUTH: p.translate( 0,-1); break;
                        case EAST:  p.translate( 1, 0); break;
                        case WEST:  p.translate(-1, 0); break;
                    }
                    Node childNode = new Node(p,goal,n,dir);
                    //System.out.println(childNode.p.toString());
                    if(!searchSet.contains(childNode) && map.containsKey(childNode.p) && !map.get(childNode.p).equals(Entity.OBSTACLE)) {
                        if(inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM) || (!map.get(childNode.p).equals(Entity.OURBASE) && !map.get(childNode.p).equals(Entity.OURFLAG) && !map.get(childNode.p).equals(Entity.TEAMMATE))) {
                            searchGraph.add(childNode);
                            System.out.println("("+p.x + "," + p.y+") ");
                        }
                    }
                }
                //searchGraph.stream().forEach(p -> System.out.print("("+p.p.x+","+p.p.y+")"));
                //System.out.println();
                
                return successors(map, searchGraph, searchSet, goal);
            }
        }
    }
    //------------------------------------------------------------------------//
    
    
    //----Print methods-------------------------------------------------------//
    private void printInfo()
    {
        System.out.println("Turn | Agent | Start Side | Base Side | moveNorth");
        System.out.printf("%4d | %5d | %10s | %9s | %s%n",this.turnNum,this.agentNum,this.startSide,ecr110030Agent.baseSide,this.moveNorth);        
    }
    
    private void printMap()
    {
        for(int y=mapSize-1; y>-1; y--)
        {
            for(int x=0; x<mapSize; x++)
            {
                System.out.print(" " + display(new Point(x,y)));
            }
            System.out.println();
        }
    }
    
    /*
    private void printMap()
    {
        String str = map.keySet().stream()
                .sorted((p1, p2) -> (p1.x - p2.x) + (mapSize+3)*(p1.y - p2.y))
                .filter(p -> p.x < mapSize)
                .filter(p -> p.y < mapSize)
                .map(p -> display(p))
                .collect(Collectors.joining(" "));
        //System.out.println(str);
        
        //map.keySet().stream().forEach((Point p) -> System.out.printf("("+p.x+","+p.y+")"+ map.get(p).toString() +"\t"));
        System.out.println();
        for (int i = str.length()-(mapSize*2-1); i >= 0; i -= mapSize*2){
           System.out.println(str.substring(i, i+(mapSize*2-1)));
        }
    } */
    
    private String display(Point p){
        char entity;
        switch(map.get(p)) {
            //case TEAMMATE:  entity = Integer.toString(this.agentNum).charAt(0); break;
            case TEAMMATE:  entity = 'T'; break;
            case ENEMY:     entity = 'E'; break;
            case OBSTACLE:  entity = '*'; break;
            case OURFLAG:   entity = '~'; break;
            case ENEMYFLAG: entity = '?'; break;
            case OURBASE:   entity = 'H'; break;
            case ENEMYBASE: entity = 'B'; break;
            default:        entity = '_'; break;
        }
        return "" + entity;
    }
    //------------------------------------------------------------------------//
    //in a 10x10 game, flag should be located at row 9/2 = 4  ex: [0,1,2,3,*4*,5,6,7,8,9]
    //as such, bottom agent should move upwards until flag is found.  Maybe have both agents
    //  move to flag and bomb around it?
}
