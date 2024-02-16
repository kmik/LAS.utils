package utils;

import java.util.HashMap;

public class zonalExtractorCounter {

    zonalExtractor zonal;
    int currentCellId = 0;
    int startCellId = 0;

    int previousId = 0;
    HashMap<Integer, zonalCell> cells = new HashMap<>();
    public zonalExtractorCounter(){

    }

    public zonalExtractorCounter(zonalExtractor zonal){
        this.zonal = zonal;
        this.cells = zonal.cells;
    }

    public void setStartCellId(int id){
        currentCellId = id;
        startCellId = id;
        previousId = id;

    }

    public boolean changed(int cellId){

        if(cellId != currentCellId) {
            previousId = currentCellId;
            this.currentCellId = cellId;

            return true;
        }else
            return false;

    }


}
