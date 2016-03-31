package main.java.com.indoor.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import main.java.com.indoor.helpers.GridSquare;

public class AStar {

	public static final int DIAGONAL_COST = 14;
    public static final int V_H_COST = 10;
    private int gridMapMatrixBlocked [][];
    private int boardRows;
    private int boardColumns;

    static class Cell{  
        int heuristicCost = 0; //Heuristic cost
        int finalCost = 0; //G+H
        int i, j;
        Cell parent; 
        
        Cell(int i, int j){
            this.i = i;
            this.j = j; 
        }
        
        @Override
        public String toString(){
            return "["+this.i+", "+this.j+"]";
        }
    }
    
    //Blocked cells are just null Cell values in grid
    static Cell [][] grid = new Cell[5][5];
    
    static PriorityQueue<Cell> open;
     
    static boolean closed[][];
    static int startI, startJ;
    static int endI, endJ;
            
    public static void setBlocked(int i, int j){
        grid[i][j] = null;
    }
    
    public static void setStartCell(int i, int j){
        startI = i;
        startJ = j;
    }
    
    public static void setEndCell(int i, int j){
        endI = i;
        endJ = j; 
    }
    
    static void checkAndUpdateCost(Cell current, Cell t, int cost){
        if(t == null || closed[t.i][t.j])return;
        int t_final_cost = t.heuristicCost+cost;
        
        boolean inOpen = open.contains(t);
        if(!inOpen || t_final_cost<t.finalCost){
            t.finalCost = t_final_cost;
            t.parent = current;
            if(!inOpen)open.add(t);
        }
    }
    
    public static void AStar1(){ 
        
        //add the start location to open list.
        open.add(grid[startI][startJ]);
        
        Cell current;
        
        while(true){ 
            current = open.poll();
            if(current==null)break;
            closed[current.i][current.j]=true; 

            if(current.equals(grid[endI][endJ])){
                return; 
            } 

            Cell t;  
            if(current.i-1>=0){
                t = grid[current.i-1][current.j];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 

                if(current.j-1>=0){                      
                    t = grid[current.i-1][current.j-1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }

                if(current.j+1<grid[0].length){
                    t = grid[current.i-1][current.j+1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }
            } 

            if(current.j-1>=0){
                t = grid[current.i][current.j-1];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 
            }

            if(current.j+1<grid[0].length){
                t = grid[current.i][current.j+1];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 
            }

            if(current.i+1<grid.length){
                t = grid[current.i+1][current.j];
                checkAndUpdateCost(current, t, current.finalCost+V_H_COST); 

                if(current.j-1>=0){
                    t = grid[current.i+1][current.j-1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }
                
                if(current.j+1<grid[0].length){
                   t = grid[current.i+1][current.j+1];
                    checkAndUpdateCost(current, t, current.finalCost+DIAGONAL_COST); 
                }  
            }
        } 
    }
    
/*
    Params :
    si, sj = start location's x and y coordinates
    ei, ej = end location's x and y coordinates
*/
    public void findShortestPath(int si, int sj, int ei, int ej){
         //Reset
        grid = new Cell[this.boardRows][this.boardColumns];
        closed = new boolean[this.boardRows][this.boardColumns];
        open = new PriorityQueue<>((Object o1, Object o2) -> {
             Cell c1 = (Cell)o1;
             Cell c2 = (Cell)o2;

             return c1.finalCost<c2.finalCost?-1:c1.finalCost>c2.finalCost?1:0;
         });
        //Set start position
        setStartCell(si, sj);  //Setting to 0,0 by default. Will be useful for the UI part
        
        //Set End Location
        setEndCell(ei, ej); 
        
        for(int i=0;i<this.boardRows;++i){
           for(int j=0;j<this.boardColumns;++j){
               grid[i][j] = new Cell(i, j);
               grid[i][j].heuristicCost = Math.abs(i-endI)+Math.abs(j-endJ);
           }
        }
        grid[si][sj].finalCost = 0;
        

        for(int i=0;i<this.gridMapMatrixBlocked.length;++i){
            setBlocked(gridMapMatrixBlocked[i][0], gridMapMatrixBlocked[i][1]);
        }
        
        //Display initial map
        System.out.println("Grid: ");
         for(int i=0;i<this.boardRows;++i){
             for(int j=0;j<this.boardColumns;++j){
                if(i==si&&j==sj)System.out.print("SO  "); //Source
                else if(i==ei && j==ej)System.out.print("DE  ");  //Destination
                else if(grid[i][j]!=null)System.out.printf("%-3d ", 0);
                else System.out.print("BL  "); 
             }
             System.out.println();
         } 
         System.out.println();
        
        AStar1(); 
        System.out.println("\nScores for cells: ");
        for(int i=0;i<this.boardRows;++i){
            for(int j=0;j<this.boardColumns;++j){
                if(grid[i][j]!=null)System.out.printf("%-3d ", grid[i][j].finalCost);
                else System.out.print("BL  ");
            }
            System.out.println();
        }
        System.out.println();
         
        if(closed[endI][endJ]){
            //Trace back the path 
             System.out.println("Path: ");
             Cell current = grid[endI][endJ];
             System.out.print(current);
             while(current.parent!=null){
                 System.out.print(" -> "+current.parent);
                 current = current.parent;
             } 
             System.out.println();
        }else System.out.println("No possible path");
 } 
    
    public AStar(GridSquare gridMapMatrix[][]) {
    	this.boardColumns = gridMapMatrix[0].length;
    	this.boardRows = gridMapMatrix.length;
    	List<int[]> rowList = new ArrayList<int[]>();
    	for(int i=0;i<gridMapMatrix.length;i++){
    		//System.out.println();
			for(int j=0;j<gridMapMatrix[i].length; j++){
				//System.out.print(gridMapMatrix[i][j].getIsInObstacle()+ " ");
				if(gridMapMatrix[i][j].getIsInObstacle()) {
					rowList.add(new int[] { i, j });
				}
			}
    	}
    	this.gridMapMatrixBlocked = new int [rowList.size()][2];
    	for (int i = 0; i < rowList.size(); i++) {
    		this.gridMapMatrixBlocked[i][0] = rowList.get(i)[0];
    		this.gridMapMatrixBlocked[i][1] = rowList.get(i)[1];
    	}
    }
    
    /*
    public static void main(String[] args) throws Exception{   
        test(1, 5, 5, 0, 0, 3, 2, new int[][]{{0,4},{2,2},{3,1},{3,3}}); 
        test(2, 5, 5, 0, 0, 4, 4, new int[][]{{0,4},{2,2},{3,1},{3,3}});   
        test(3, 7, 7, 2, 1, 5, 4, new int[][]{{4,1},{4,3},{5,3},{2,3}});
        
        test(1, 5, 5, 0, 0, 4, 4, new int[][]{{3,4},{3,3},{4,3}});
    }
	*/
}
