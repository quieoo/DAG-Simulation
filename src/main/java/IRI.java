import java.security.SecureRandom;
import java.util.*;

public class IRI {
    public Tangle tangle;
    public CumulativeWeightCalculator cwc;
    public WalkerAlpha w;
    public TxSender sender;
    public List<IRI> neighbour;
    private int latency;

    public IRI(){
        tangle=new Tangle();
        cwc=new CumulativeWeightCalculator(tangle);
        w=new WalkerAlpha(tangle,new SecureRandom());
        neighbour=new LinkedList<>();
    }
    public void Config(int _latency){
        latency=_latency;
    }

    public void NewNeighbours(List<IRI> _neighbours){
        for(IRI i:_neighbours){
            if(i!=this){
                neighbour.add(i);
            }
        }
    }
    public void IncomingTx(Transaction tx) throws InterruptedException {
        Thread.sleep(latency);
        tangle.StoreTransaction(tx);
    }
    public void BroadcastTransaction(Transaction tx) throws InterruptedException {
        for(IRI n:neighbour){
            n.IncomingTx(tx);
        }
    }

    public List<Integer> SeenTips(Map<Integer,Integer> weightMap){
        List<Integer> result=new ArrayList<>();
        for(Map.Entry<Integer,Integer> e:weightMap.entrySet()){
            if (e.getValue().equals(1)){
                result.add(e.getKey());
            }
        }
        return result;
    }
    public List<Integer> GetTransactionToApprove(int depth, Optional<Integer> reference) throws Exception {
        int entry=GetEntryPoint(depth);
        Map<Integer, Integer> wm= cwc.calculate(entry);
        if(sender.name=="coo"){
            //calculate weight of specific node
            Map<Integer,Integer> wmm=cwc.calculate(tangle.TxSequence.get(0));
            for(int i=0;i<100;i++){
                if(tangle.TxSequence.size()>i+1 && wmm.containsKey(tangle.TxSequence.get(i))){
                    System.out.printf("%d ",wmm.get(tangle.TxSequence.get(i)));
                }
            }
            System.out.printf("\n");
        }
        //log all seen tips
        List<Integer> Seen = new ArrayList<>();
        Seen=SeenTips(wm);
        //System.out.printf("%s: %s\n",sender.name,Seen.toString());


        List<Integer> tips = new ArrayList<>();
        Integer tip = w.walk(entry, wm);
        tips.add(tip);

        if (reference.isPresent()) {
            entry = reference.get();
        }

        tip=w.walk(entry,wm);
        tips.add(tip);

        return tips;
    }


    public int GetEntryPoint(int depth){
        int left=depth;
        int it=tangle.TxSequence.size()-1;
        Integer lastMilestone=0;
        while(left > 0 && it>=0){
            Transaction tx=tangle.map.get(tangle.TxSequence.get(it));
            if(tx.Milestone){
                lastMilestone=tx.hash;
                left--;
            }
            it--;
        }
        return lastMilestone;
    }
    public void NewSender(String n, int d, int t, boolean c){
        sender=new TxSender(n,d,t,c);
    }
    public  void StartSender(int priority){
        sender.start(priority);
    }

    public class TxSender implements Runnable{
        int depth;
        int tick;
        private Thread t;
        boolean isCoordinator;
        String name;

        public TxSender(String n,int d, int t, boolean c){
            name=n;
            depth=d;
            tick=t;
            isCoordinator=c;
        }

        public void start(int priority){
            System.out.println(name+" start sender.");
            if (t == null) {
                t = new Thread (this, name);
                t.setPriority(priority);
                t.start ();
            }
        }

        public void run(){
            boolean bootstrap=true;
            int bootstrapStage = 0;
            Integer trunk,branch;

            while(true){
                //tangle.Print();
                // get tips to apptove
                if(isCoordinator){
                    if(!bootstrap){
                        try{
                            //System.out.printf("%s %s\n",sender.name, "begin GTTA");
                            List<Integer> tips=GetTransactionToApprove(depth, Optional.of(tangle.GetLatestMilestone()));
                            //System.out.printf("%s %s\n",sender.name,tips.toString());
                            trunk=tips.get(0);
                            branch= tips.get(1);
                        }catch(Exception e){
                            trunk=tangle.GetLatestMilestone();
                            branch=tangle.GetLatestMilestone();
                        }
                    }else{
                        if (bootstrapStage >= 3) {
                            bootstrap=false;
                            continue;
                        }
                        if (bootstrapStage == 0) {
                            System.out.println("Bootstrapping network.");
                            trunk = 0;
                            branch = 0;
                            bootstrapStage = 1;
                        } else {
                            // Bootstrapping means creating a chain of milestones without pulling in external transactions.
                            System.out.println("Reusing last milestone.");
                            trunk = tangle.GetLatestMilestone();
                            branch = 0;
                            bootstrapStage++;
                        }
                    }
                }else{
                    List<Integer> tips= null;
                    try {
                        tips = GetTransactionToApprove(depth,Optional.ofNullable(null));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    trunk=tips.get(0);
                    branch= tips.get(1);
                }

                //store transaction
                Transaction tx=new Transaction(branch,trunk,isCoordinator);
                boolean valid=tangle.StoreTransaction(tx);

                if(valid){
                    //broadcast
                    try {
                        BroadcastTransaction(tx);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


                //sleep
                try {
                    Thread.sleep(tick);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
