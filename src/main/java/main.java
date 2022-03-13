import java.security.SecureRandom;
import java.util.*;

public class main {
    public static void main(String[] args) throws Exception {
        //maptest();
        List<IRI> nodes=new ArrayList<>();
        int num=10;
        int tick=10;

        Random rand =new Random(25);

        for(int i=0;i<num;i++){
            IRI node =new IRI();
            if(i==0){
                node.NewSender("coo",3,3*tick,true);
                node.Config(tick);
            }else{
                node.NewSender(String.format("c%d",i),3,tick,false);
                node.Config(rand.nextInt(10));
            }
            nodes.add(node);
        }

        for(IRI i:nodes){
            i.NewNeighbours(nodes);
        }

        nodes.get(0).StartSender(10);
        Thread.sleep(10*tick);
        for(int i=1;i<num;i++){
            nodes.get(i).StartSender(5);
        }
    }

}
