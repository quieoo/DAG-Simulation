import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Tangle {


    ConcurrentHashMap<Integer,Transaction> map;
    List<Integer> TxSequence;
    int latestMilestone;

    public Tangle(){
        map=new ConcurrentHashMap<>();
        TxSequence=new ArrayList<>();
        latestMilestone=0;
    }
    public HashSet<Integer> GetApprovers(Integer t){
        HashSet<Integer> approvers=new HashSet<>();

        for(Map.Entry<Integer,Transaction> entry: map.entrySet()){
            if(Objects.equals(entry.getKey(), t)){
                approvers.addAll(entry.getValue().approvee);
            }
        }
        return approvers;
    }

    public int GetLatestMilestone(){
        return latestMilestone;
    }

    public boolean StoreTransaction(Transaction tx){
        boolean validTx=true;

        //check tx exist before store transaction
        if(!map.containsKey(tx.hash)){
            map.put(tx.hash,tx);
            TxSequence.add(tx.hash);
            if(tx.Milestone){
                latestMilestone=tx.hash;
            }

            if(map.containsKey(tx.trunk)){
                Transaction tn=map.get(tx.trunk);
                tn.approvee.add(tx.hash);
                map.put(tn.hash,tn);
            }
            if(map.containsKey(tx.branch)){
                Transaction bn=map.get(tx.branch);
                bn.approvee.add(tx.hash);
                map.put(bn.hash,bn);
            }
        }else{
            validTx=false;
        }

        //System.out.printf("tx:%d, trunk:%d, branch:%d, milestone:%b\n",tx.hash,tx.trunk,tx.branch,tx.Milestone);

        return validTx;
    }

    public void Print(){
        for(Integer h:TxSequence){
            Transaction tx=map.get(h);
            System.out.printf("tx:%d, trunk:%d, branch:%d, milestone:%b\n",tx.hash,tx.trunk,tx.branch,tx.Milestone);
        }
    }
}
