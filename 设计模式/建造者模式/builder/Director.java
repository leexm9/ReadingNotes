package builder;

import java.util.ArrayList;

public class Director {
    private ArrayList<String> sequence = new ArrayList<String>();
    private BWMBuilder bwmBuilder = new BWMBuilder();
    private AudiBuilder audiBuilder = new AudiBuilder();
    
    /**
     * 1号类型，先start，后stop；
     */
    public BWM getBWM1() {
        //清理场景
        this.sequence.clear();
        //1类型组装
        this.sequence.add("start");
        this.sequence.add("stop");
        //返回模型
        this.bwmBuilder.setSequence(sequence);
        return (BWM) this.bwmBuilder.getCarModel();
    }
    
    /**
     * 2号类型，先引擎，后启动，后stop
     */
    public BWM getBWM2() {
        this.sequence.clear();
        this.sequence.add("engine boom");
        this.sequence.add("start");
        this.sequence.add("stop");
        this.bwmBuilder.setSequence(sequence);
        return (BWM) this.bwmBuilder.getCarModel();
    }
    
    public Audi getAudi1() {
        this.sequence.clear();
        this.sequence.add("alarm");
        this.sequence.add("start");
        this.sequence.add("stop");
        this.audiBuilder.setSequence(sequence);
        return (Audi) this.audiBuilder.getCarModel();
    }
    
    public Audi getAudi2() {
        this.sequence.clear();
        this.sequence.add("start");
        this.audiBuilder.setSequence(sequence);
        return (Audi) this.audiBuilder.getCarModel();
    }
}
