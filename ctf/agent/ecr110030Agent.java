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
    private static int maxAgentNum = 0;
    private static Direction baseSide = Direction.EAST;
    private static boolean determinedBaseSide = false;
    private static int mapSize = 10;
    private static boolean determinedMapSize = false;
    private static Point goal;
    private static boolean foundGoal = false;
    private static HashMap<Point, Entity> map = new HashMap<>();
    
    //Any static variables that must be reset (almost all) should be re-assigned here
    private void resetStaticVariables() { staticVariablesReset = true;
        maxAgentNum = agentNumInitializer;
        agentNumInitializer = 0;    //gets reset after agents have been constructed for a team
        baseSide = Direction.EAST;
        determinedBaseSide = false;
        mapSize = 10;
        determinedMapSize = false;
        goal = new Point(0,4);
        foundGoal = false;
        map = new HashMap<>();
    }
    
    //----Private local Agent variables and constructor----//
    private final int agentNum;
    private int turnNum;
    private Point location;
    AgentEnvironment inEnvironment; //set every time getMove is called
    //must only use no-arg constructor
    public ecr110030Agent(){
        agentNum = agentNumInitializer++;
        turnNum = 0;
        location = new Point();
    }
    
    //Ideas:
    //- try to determine location of each agent, and map it
    //- map environment
    //- determine furthest east/west unblocked opening until enemy base is discovered
            
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
            if(!staticVariablesReset) resetStaticVariables();
            localMap.put(localStartingLocation, Entity.TEAMMATE);
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
        if(!determinedBaseSide) baseSide = inEnvironment.isBaseWest(AgentEnvironment.ENEMY_TEAM, false) ? Direction.WEST : Direction.EAST;//
        //initialization of map size and grid                                       //
        if(moveNorth){ //calculates map size, assuming map size is even integer.   //
          //printShit();                                                          //
            startingColumnSize++;                                                //
            if(inEnvironment.isFlagNorth(AgentEnvironment.OUR_TEAM, true)) {    //
                moveNorth = false;                                             //
                determinedMapSize = true;                                     //
                mapSize = 2*(startingColumnSize+(maxAgentNum/2 - 1));        //
                //initialize map                                            //
                for(int j=0; j<mapSize; j++) {                             //
                    for(int k=0; k<mapSize; k++) {                        //
                        map.put(new Point(j,k), Entity.EMPTY);}
                location = new Point(baseSide==Direction.EAST ? mapSize-1 : 0, (mapSize-1)/2 - 1);
                }         //
            } else {                                                    //
                this.scanAround(localMap);                             //
                return this.localMove(Direction.NORTH);}}             //
        //===========================================================//
        //locally store data until map size has been determined
        if(!determinedMapSize) {
            this.scanAround(localMap);  //scan around current location
            System.out.println();
            if(!inEnvironment.isObstacleWestImmediate())
                return this.localMove(Direction.WEST); //make a local move and update old/new locations
        }
        //now, migrate agent data to shared map
        if(determinedMapSize) {
            //migrate data from localMap to map
            if(migrateData) { migrateData = false;
                //reminder: agents are initialzed from top to bottom 
                Point actualPoint = new Point(baseSide==Direction.WEST ? mapSize-1 : 0, agentNum <= (maxAgentNum-1)/2 ? mapSize-1-agentNum : 0+maxAgentNum-1-agentNum);
                localMap.keySet().forEach((Point p) -> {
                    Point newP = new Point(p);
                    newP.translate(actualPoint.x, actualPoint.y);
                    map.put(newP,localMap.get(p));
                });
                Point newP = new Point(location);
                newP.translate(actualPoint.x, actualPoint.y);
                this.location = newP;
            }
            //System.out.println("(" + this.location.x + ',' + this.location.y + ')');
            printMap();
            
            /** 
             * AI logic goes here. 
             */
            //if this agent does not have the flag
            if(!inEnvironment.hasFlag()){
                //if a teammate has the enemy flag
                if(inEnvironment.hasFlag(AgentEnvironment.OUR_TEAM)){
                    
                } //else head for the flag 
                else {
                    
                    //use A*? f(n) = g(n) + h(n) where h(n) <= h*(n)
                    this.scanAround(map);
                    return this.move(astar(map));
                    
                }
                
                
                if(inEnvironment.hasFlag(AgentEnvironment.ENEMY_TEAM)) {
                    
                }
            }
            
            
            //if this agent has the flag
            if(inEnvironment.hasFlag()) {
                
            }
            
            
            
            
        }
        
        //do nothing for now once map initialized...
        return AgentAction.DO_NOTHING;
    }
    
    
    //----Methods for agent movement and updating map(s)----------------------//
    private int move(Direction direction)
    {
        map.put(new Point(location), Entity.EMPTY);
        Point newLocation = new Point(location);
        switch(direction) {
            case NORTH: newLocation.translate( 0, 1); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_NORTH;
            case SOUTH: newLocation.translate( 0,-1); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_SOUTH;
            case EAST:  newLocation.translate( 1, 0); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_EAST;
            case WEST:  newLocation.translate(-1, 0); map.put(newLocation, Entity.TEAMMATE);
                        location = newLocation; return AgentAction.MOVE_WEST;
            default:    map.put(new Point(location), Entity.ENEMY); return AgentAction.DO_NOTHING;
        }
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
            case NORTH: tile=inEnvironment.isObstacleNorthImmediate() ? Entity.OBSTACLE 
                        : inEnvironment.isFlagNorth(AgentEnvironment.OUR_TEAM,true) ? Entity.OURFLAG 
                        : inEnvironment.isFlagNorth(AgentEnvironment.ENEMY_TEAM,true) ? Entity.ENEMYFLAG 
                        : inEnvironment.isAgentNorth(AgentEnvironment.ENEMY_TEAM,true) ? Entity.ENEMY 
                        : inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM,true) ? Entity.OURBASE
                        : Entity.EMPTY; break;
            case SOUTH: tile=inEnvironment.isObstacleSouthImmediate() ? Entity.OBSTACLE 
                        : inEnvironment.isFlagSouth(AgentEnvironment.OUR_TEAM,true) ? Entity.OURFLAG 
                        : inEnvironment.isFlagSouth(AgentEnvironment.OUR_TEAM,true) ? Entity.ENEMYFLAG 
                        : inEnvironment.isAgentSouth(AgentEnvironment.ENEMY_TEAM,true) ? Entity.ENEMY 
                        : inEnvironment.isBaseSouth(AgentEnvironment.OUR_TEAM,true) ? Entity.OURBASE
                        : Entity.EMPTY; break;
            case EAST:  tile=inEnvironment.isObstacleEastImmediate() ? Entity.OBSTACLE 
                        : inEnvironment.isFlagEast(AgentEnvironment.OUR_TEAM,true) ? Entity.OURFLAG 
                        : inEnvironment.isFlagEast(AgentEnvironment.OUR_TEAM,true) ? Entity.ENEMYFLAG 
                        : inEnvironment.isAgentEast(AgentEnvironment.ENEMY_TEAM,true) ? Entity.ENEMY 
                        : inEnvironment.isBaseEast(AgentEnvironment.OUR_TEAM,true) ? Entity.OURBASE
                        : Entity.EMPTY; break;
            case WEST:  tile=inEnvironment.isObstacleWestImmediate() ? Entity.OBSTACLE 
                        : inEnvironment.isFlagWest(AgentEnvironment.OUR_TEAM,true) ? Entity.OURFLAG 
                        : inEnvironment.isFlagWest(AgentEnvironment.OUR_TEAM,true) ? Entity.ENEMYFLAG 
                        : inEnvironment.isAgentWest(AgentEnvironment.ENEMY_TEAM,true) ? Entity.ENEMY 
                        : inEnvironment.isBaseWest(AgentEnvironment.OUR_TEAM,true) ? Entity.OURBASE
                        : Entity.EMPTY; break;
        }
        return tile;
    } 
    //------------------------------------------------------------------------//
    
    
    //----Methods for determining where to move-------------------------------//
    private class Node{
        Point p = new Point();
        Node parent;
        Direction moveDir; //direction taken to reach this node
        int f, g, h;
        
        private Node(Point thisPoint) { p = thisPoint; g = 0; h = manhattanDistance(); f = g + h; }
        private Node(Point thisPoint, Node parentNode, Direction dir) {
            p = thisPoint;
            parent = parentNode;
            moveDir = dir;
            g = this.parent.g+1;
            h = manhattanDistance();
            f = this.g + this.h;
            //System.out.println("Point: ("+p.x+","+p.y+"); f="+f+", g="+g+", h="+h);
        }
        private int manhattanDistance() {
            //System.out.println("Mh dist: ("+goal.c.x+","+goal.c.y+") - ("+this.p.x+","+this.p.y+") = "+(Math.abs(goal.c.x - this.p.x) + Math.abs(goal.c.y - this.p.y)));
            return Math.abs(goal.x - this.p.x) + Math.abs(goal.y - this.p.y); 
        }
        @Override
        public boolean equals(Object o)
        {
            if(o.getClass() != this.getClass()) return true;
            return this.p.equals(o);
        }
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + Objects.hashCode(this.p);
            return hash;
        }
    }
    
    private Direction astar(HashMap<Point, Entity> map)
    {
        //initialize priority queue and associated set
        PriorityQueue<Node> searchGraph = new PriorityQueue<>(
                (Node n1, Node n2) -> { 
                    return n1.f == n2.f ? n1.h == n2.h ? n1.p.x - n2.p.x : n1.h - n2.h : n1.f - n2.f; 
        });
        //maybe duplicates don't matter?
        //HashSet<Node> searchSet = new HashSet<>();
        searchGraph.add(new Node(location));
        //goalLeaf = null if no path found, which will not happen since duplicates are allowed for now
        Node goalLeaf = successors(map, searchGraph);
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
    
    private Node successors(HashMap<Point, Entity> map, PriorityQueue<Node> searchGraph)
    {
        if(searchGraph.isEmpty()) return null;
        else {
            Node n = searchGraph.poll();
            System.out.println("Selected: "+n.p.toString());
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
                    Node childNode = new Node(p,n,dir);
                    if( (p.x > -1) && (p.x < mapSize) && (p.y > -1) && (p.y<mapSize) 
                            && !map.get(childNode.p).equals(Entity.OBSTACLE) 
                            && !map.get(childNode.p).equals(Entity.OURBASE))
                        searchGraph.add(childNode);
                }
                //searchGraph.stream().forEach(p -> System.out.print("("+p.p.x+","+p.p.y+")"));
                //System.out.println();
                
                return successors(map, searchGraph);
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
