import java.util.Set;
import java.util.concurrent.Callable;

class ParallelTask implements Callable<String>
{
	private String label;
	private BidirCondCounts<String, String> nodeAligns;
	private boolean srcSide;

	public ParallelTask(String label, BidirCondCounts<String, String> nodeAligns, boolean srcSide)
	{
		this.label = label;
		this.nodeAligns = nodeAligns;
		this.srcSide = srcSide;
	}

	public String call()
	{
		Set<String> labels;
		if(srcSide)
			labels = nodeAligns.GetSrcItems();
		else
			labels = nodeAligns.GetTgtItems();

		return LabelCollapser.getClosestLabelByL1( this.label, this.nodeAligns, labels, this.srcSide );
	} 
}
