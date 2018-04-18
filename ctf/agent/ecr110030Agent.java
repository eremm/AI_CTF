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
import java.util.stream.Collectors;

/**
 *
 * @author evan
 */
public class ecr110030Agent extends Agent {
    
    //----Static class variables used to communicate between agents----//
    //Must be reset between every run since each match only has 1 round
    private static enum Direction { NORTH, SOUTH, EAST, WEST; }
    private static enum Entity { TEAMMATE, ENEMY, OBSTACLE, EMPTY, OURFLAG, ENEMYFLAG, OURBASE, ENEMYBASE; }
    private static int agentNumInitializer = 0;
    private static int maxAgentNum = 0;
    private static boolean staticVariablesReset = false;
    private static Direction baseSide = Direction.EAST;
    private static boolean determinedBaseSide = false;
    private static int mapSize = 10;
    private static boolean determinedMapSize = false;
    private static HashMap<Point, Entity> map = new HashMap<>();
    
    //Any static variables that must be reset (almost all) should be re-assigned here
    private void resetStaticVariables() { staticVariablesReset = true;
        maxAgentNum = agentNumInitializer + 0;
        agentNumInitializer = 0;    //gets reset after agents have been constructed for a team
        baseSide = Direction.EAST;
        determinedBaseSide = false;
        mapSize = 10;
        determinedMapSize = false;
    }
    
    //----Private local Agent variables and constructor----//
    private final int agentNum;
    private int turnNum;
    private Coordinate location;
    AgentEnvironment inEnvironment; //set every time getMove is called
    //must only use no-arg constructor
    public ecr110030Agent(){
        agentNum = agentNumInitializer++;
        turnNum = 0;
        location = new Coordinate(0,0);
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
    private Coordinate localStartingCoordinate = new Coordinate(0,0);
    private HashMap<Point, Entity> localMap = new HashMap<>();
    private boolean migrateData = true;
    //------------------------------------------------------------------------//
    
    
    //----Inner class(es) for automating scan process-------------------------//
    private class Coordinate {
        Point c = new Point();
        Entity entity = Entity.TEAMMATE;
        private Coordinate() {};
        private Coordinate(int x, int y) {
            this.c.x = x; this.c.y = y; }
        private Coordinate(Coordinate parent, Direction adjacent, Entity entity) {
            this.entity = entity;
            this.c.setLocation(parent.c);
            switch(adjacent) {
                case NORTH: c.translate( 0, 1); break;
                case SOUTH: c.translate( 0,-1); break;
                case EAST:  c.translate( 1, 0); break;
                case WEST:  c.translate(-1, 0); break;
                default:    c.translate( 0, 0); break; 
            }
        }
        //
        private void scanAround(HashMap<Point, Entity> map){ //use hashmap instead with points and coords?
            Coordinate north = this.scanAdjacent(Direction.NORTH);
            Coordinate south = this.scanAdjacent(Direction.SOUTH);
            Coordinate east  = this.scanAdjacent(Direction.EAST);
            Coordinate west  = this.scanAdjacent(Direction.WEST);
            //apply cluster to map
            map.put(north.c,north.entity); map.put(south.c, south.entity); map.put(east.c, east.entity); map.put(west.c, west.entity);
        }
        //scans immediate surroundings of agent
        private Coordinate scanAdjacent(Direction direction){
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
            return new Coordinate(this, direction, tile);
        } 
        //may not need these
        @Override
        public boolean equals(Object o){
            if(o.getClass() != Coordinate.class) return false;
            Coordinate coord = (Coordinate) o;
            return this.c.equals(coord.c);
        }
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 31 * hash + Objects.hashCode(this.c);
            hash = 31 * hash + Objects.hashCode(this.entity);
            return hash;
        }
    } 
    //------------------------------------------------------------------------//
    
    
    //determines which side of base agents are on and decides if agent needs to calculate map size
    private void firstMoveAndSouthSideCounts(Coordinate start) {
        if(firstMove) { firstMove = false;
            startSide = inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM, false) ? Direction.SOUTH : Direction.NORTH;
            moveNorth = (startSide==Direction.SOUTH && !inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM,true));
            if(!staticVariablesReset) resetStaticVariables();
            localMap.put(start.c, start.entity);
            System.out.println("-----");
        } else if(staticVariablesReset) staticVariablesReset = false;
    }
    
    private int action(Direction direction)
    {
        switch(direction) {
            case NORTH: return AgentAction.MOVE_NORTH;
            case SOUTH: return AgentAction.MOVE_SOUTH;
            case EAST:  return AgentAction.MOVE_EAST;
            case WEST:  return AgentAction.MOVE_WEST;
            default: return AgentAction.DO_NOTHING;
        }
    }
    
    private int localMove(Direction direction)
    {
        Coordinate nextMove = new Coordinate(this.location, direction, Entity.TEAMMATE);
        localMap.put(location.c, Entity.EMPTY);
        this.location = nextMove;
        localMap.put(nextMove.c, nextMove.entity);
        return this.action(direction);
    }
    
    private int move(Direction direction)
    {
        Coordinate nextMove = new Coordinate(this.location, direction, Entity.TEAMMATE);
        map.put(location.c, Entity.EMPTY);      //old location is empty
        this.location = nextMove;           
        map.put(nextMove.c, nextMove.entity);   //new location = nextMove
        return this.action(direction);
    }
    
    
    /**
     * Calculates the next move for an agent.
     * @param inEnvironment the environment in which the agent is in
     * @return action which to take
     */
    @Override
    public int getMove(AgentEnvironment inEnvironment) {
        this.inEnvironment = inEnvironment; turnNum++; System.out.println();
        firstMoveAndSouthSideCounts(localStartingCoordinate);                                               
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
                        map.put(new Point(j,k), Entity.EMPTY);}}         //
            } else {                                                    //
                //printInfo();
                //localMap.keySet().stream().forEach((Point p) -> System.out.printf("("+p.x+","+p.y+")\t"));
                return AgentAction.MOVE_NORTH;}}                       //
        //============================================================//
        //locally store data until map size has been determined
        if(!determinedMapSize) {
            location.scanAround(localMap);  //scan around current location
            //printInfo();
            //localMap.keySet().stream().forEach((Point p) -> System.out.printf("("+p.x+","+p.y+")\t"));
            System.out.println();
            if(!inEnvironment.isObstacleWestImmediate()){
                return this.localMove(Direction.WEST);  //make a local move and update old/new locations
            }
        }
        //now, migrate agent data to shared map
        if(determinedMapSize) {
            //migrate data from localMap to map
            if(migrateData) { migrateData = false;
                //reminder: agents are initialzed from top to bottom 
                Coordinate actualCoordinate = new Coordinate(baseSide==Direction.WEST ? mapSize-1 : 0, agentNum <= (maxAgentNum-1)/2 ? mapSize-1-agentNum : 0+maxAgentNum-1-agentNum);
                localMap.keySet().forEach((Point p) -> {
                    Entity e = localMap.get(p);
                    Point newP = new Point(p);
                    newP.translate(actualCoordinate.c.x, actualCoordinate.c.y);
                    map.put(newP,e);
                });
                Point newP = new Point(location.c);
                newP.translate(actualCoordinate.c.x, actualCoordinate.c.y);
                this.location.c = newP;
            }
            
            //printInfo();
            //map.keySet().stream().forEach((Point p) -> System.out.printf("("+p.x+","+p.y+")"+ map.get(p).toString() +"\t"));
            //System.out.println();
            
            printMap();
            
            /** 
             * AI logic goes here. 
             */
            
            
        }
        
        //do nothing for now once map initialized...
        return AgentAction.DO_NOTHING;
    }
    
    
    private void printInfo()
    {
        System.out.println("Turn | Agent | Start Side | Base Side | moveNorth");
        System.out.printf("%4d | %5d | %10s | %9s | %s%n",this.turnNum,this.agentNum,this.startSide,ecr110030Agent.baseSide,this.moveNorth);        
    }
    
    private void printMap()
    {
        String str = map.keySet().stream()
                .sorted((p1, p2) -> (p1.x - p2.x) + (mapSize+3)*(p1.y - p2.y))
                .filter(p -> p.x < mapSize)
                .filter(p -> p.y < mapSize)
                .map(p -> display(p))
                .collect(Collectors.joining(" "));
        
        map.keySet().stream().forEach((Point p) -> System.out.printf("("+p.x+","+p.y+")"+ map.get(p).toString() +"\t"));
        System.out.println();
        for (int i = str.length()-20; i > 0; i -= 20){
           System.out.println(str.substring(i, i+20));
        }
    }
    
    private String display(Point p){
        char entity;
        switch(map.get(p)) {
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
    
    //in a 10x10 game, flag should be located at row 9/2 = 4  ex: [0,1,2,3,*4*,5,6,7,8,9]
    //as such, bottom agent should move upwards until flag is found.  Maybe have both agents
    //  move to flag and bomb around it?
}
