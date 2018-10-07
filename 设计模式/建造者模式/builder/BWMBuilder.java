package builder;

import java.util.ArrayList;

public class BWMBuilder extends CarBuilder {
    
    private BWM bwm = new BWM();
    @Override
    public void setSequence(ArrayList<String> sequence) {
        this.bwm.setSequence(sequence);
    }

    @Override
    public CarModel getCarModel() {
        return this.bwm;
    }

}
