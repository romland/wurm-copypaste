package com.friya.wurmonline.server.copypaste;

import java.util.logging.Logger;

import com.wurmonline.server.Point;

class Matrix
{
	private static Logger logger = Logger.getLogger(Area.class.getName());

	private Point[][] matrix;
    final int size;
    private Point base = null;
    int degrees = 0;


    /**
     * NOTE: Both axis must be of same length.
     * 
     * @param arr
     * @param base
     */
    public Matrix(Point[][] arr, Point base)
    {
    	this.size = arr.length;
    	this.matrix = arr;
    	this.base = base;
    }

    
    public Matrix rotate90()
    {
        Point[][] temp = new Point[size][size];

        for (int i=0;i<size;i++)
            for (int j=0;j<size;j++)
                temp[i][j] = matrix[size-1-j][i];

        matrix = temp;

        degrees = (degrees + 90) % 360;
        return this;
    }
    
    
    public Matrix rotate180()
    {
        Point[][] temp = new Point[size][size];

        for (int i=0;i<size;i++)
            for (int j=0;j<size;j++)
                temp[i][j] = matrix[size-1-i][size-1-j];

        matrix = temp;

        degrees = (degrees + 180) % 360;
        return this;
    }

    
    public Matrix rotate270()
    {
    	Point[][] temp = new Point[size][size];

        for (int i=0;i<size;i++)
            for (int j=0;j<size;j++)
                temp[i][j] = matrix[j][size-1-i];

        matrix = temp;

        degrees = (degrees + 270) % 360;
        return this;
    }

    
    public Matrix transpose()
    {
        for (int i=0; i<size-1; i++) {
            for (int j=i+1; j<size; j++) {
                Point tmp = matrix[i][j];
                matrix[i][j] = matrix[j][i];
                matrix[j][i] = tmp;
            }
        }
        return this;
    }
    
    
    public Matrix flipVertical()
    {
        for (int i=0; i<size; i++) {
            for (int j=0; j<size/2; j++) {
                Point tmp = matrix[i][size-1-j];
                matrix[i][size-1-j] = matrix[i][j];
                matrix[i][j] = tmp;
            }
        }
        return this;
    }

    
    public Matrix flipHorizontal()
    {
        for (int i=0; i<size/2; i++) {
            for (int j=0; j<size; j++) {
                Point tmp = matrix[size-1-i][j];
                matrix[size-1-i][j] = matrix[i][j];
                matrix[i][j] = tmp;
            }
        }
        return this;
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<size;i++) {
            for (int j=0;j<size;j++) {
                sb.append("|");
                sb.append(matrix[i][j].getX() + "," + matrix[i][j].getY());
                if (size > 3) {
                    sb.append("\t");
                }
            }
            sb.append("|\r\n");
        }

        return sb.toString();
    }
    

    public Point getPoint(int x, int y)
    {
    	return matrix[x + base.getX()][y + base.getY()];
    }
}
