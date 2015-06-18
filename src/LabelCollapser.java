import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class LabelCollapser
{
	private static ExecutorService executor = null;
	private static int numThreads = 1;

	public static void main(String[] args)
	{
		// Collapsing types
		boolean useSrc = true;
		boolean useTgt = true;

		// Collapsing metric
		boolean useL1 = true;

		// Stopping condition
		boolean useIters = true;
		int maxValue = 0;

		// Input
		boolean useSR = true;
		boolean useOneline = false;
		boolean useMoses = false;
		String inputFile = "";
		String mosesFile1 = "";
		String mosesFile2 = "";

		int nextArg = 0;
		while(nextArg < args.length)
		{
			if(args[nextArg].toLowerCase().equals("--side")) {
				nextArg++;
				String side = args[nextArg].toLowerCase();
				if(side.equals("src"))
					useTgt = false;
				else if(side.equals("tgt"))
					useSrc = false;
				else if(side.equals("both"))
					; // Do nothing
				else
				{
					PrintUsage();
					return;
				}
			}
			else if(args[nextArg].toLowerCase().equals("--metric")) {
				nextArg++;
				String metric = args[nextArg].toLowerCase();
				if(metric.equals("kl"))
					useL1 = false;
				else if(metric.equals("l1"))
					useL1 = true;
				else
				{
					PrintUsage();
					return;
				}

			}
			else if(args[nextArg].toLowerCase().equals("--stop")) {
				nextArg++;
				String type = args[nextArg].toLowerCase();
				if(type.equals("iters"))
					maxValue = Integer.parseInt(args[++nextArg]);
				else if(type.equals("labels"))
				{
					maxValue = Integer.parseInt(args[++nextArg]);
					useIters = false;
				}
				else
				{
					PrintUsage();
					return;
				}
			}
			else if(args[nextArg].toLowerCase().equals("--input")) {
				nextArg++;
				String type = args[nextArg].toLowerCase();
				inputFile = args[++nextArg];
				if(type.equals("oneline"))
				{
					useSR = false;
					useOneline = true;
				}
				else if(type.equals("counts"))
				{
					useSR = false;
					useOneline = false;
				}
				else if(type.equals("moses"))
				{
					useSR = false;
					useOneline = false;
					useMoses = true;
					mosesFile1 = args[++nextArg];
					mosesFile2 = args[++nextArg];
				}
				else
				{
					PrintUsage();
					return;
				}
			}
			else if(args[nextArg].toLowerCase().equals("--cores")) {
				numThreads = Integer.parseInt(args[++nextArg]);
				if(numThreads <= 0)
				{
					PrintUsage();
					return;
				}
			}
			nextArg++;
		}

		// Output config info for debugging
		System.err.println(useSrc);
		System.err.println(useTgt);

		System.err.println(useL1);

		System.err.println(useIters);
		System.err.println(maxValue);
	
		System.err.println(useSR);
		System.err.println(useOneline);
		System.err.println(useMoses);
		System.err.println(inputFile);
		System.err.println(mosesFile1);
		System.err.println(mosesFile2);

		
		// Read in and count up source--target node alignments:
		BidirCondCounts<String, String> nodeAligns =
			new BidirCondCounts<String, String>(useTgt, useSrc);
			//new BidirCondCounts<String, String>(true, true);
		try
		{
			if(useSR)
				InputReader.FillFromScorableRuleFile(nodeAligns, inputFile, true, false, true);
			else if(useOneline)
				InputReader.FillFromOnelineFile(nodeAligns, inputFile);
			else if(useMoses)
				InputReader.FillFromMosesFiles(nodeAligns, inputFile, mosesFile1, mosesFile2);
			else
				InputReader.FillFromCountsFile(nodeAligns, inputFile);
		}
		catch(FileNotFoundException e)
		{
			if(useMoses)
			{
				System.err.println("Input files " + inputFile + " and " +
						mosesFile1 + " and " + mosesFile2 + " weren't all found!");
			}
			else
				System.err.println("Input file " + inputFile + " wasn't found!");
			return;
		}
		
		// Actually do the label collapsing:
		//nodeAligns.PrintNormDistributionMatrix();
		if(useL1)
		{
			if(useIters)
				RunGreedyL1Collapse(nodeAligns, maxValue, useSrc, useTgt);
			else
				System.err.println("NOT IMPLEMENTED YET!");
				// based on max num joint labels -- not written!
		}
		else
		{
			if(useIters)
				RunGreedyKLCollapse(nodeAligns, maxValue, useSrc, useTgt);
			else
				System.err.println("NOT IMPLEMENTED YET!");
				// based on max num joint labels -- not written!
		}
		//nodeAligns.PrintNormDistributionMatrix();
	}
	
	
	public static void PrintUsage()
	{
		System.err.println("Usage:  java LabelCollapser " +
				"--type <type> --metric <metric> --stop <stop> --input <input> [--cores <cores>]\n");
		System.err.println("<type>   : 'src' for source-side collapsing only");
		System.err.println("           'tgt' for target-side collapsing only");
		System.err.println("           'both' for mixed source- and target-side collapsing");
		System.err.println("<metric> : 'l1' for L1 distance");
		System.err.println("           'kl' for KL divergence");
		System.err.println("<stop>   : 'iters <num-iters>' for fixed number of iterations");
		System.err.println("           'labels <max-joint-labels>' for max final number of joint labels");
		System.err.println("<input>  : 'sr <sr-file>' for ScorableRule format");
		System.err.println("           'oneline <oneline-file>' for Vamshi oneline format");
		System.err.println("           'counts <counts-file>' for file of label co-occurence counts");
		System.err.println("           'moses <src-trees> <tgt-trees> <moses-aligns>' for parses and Moses aligns");
		System.err.println("<cores>  : 'n' to use n cores");
	}


	/*
	public static void
	RunFullGreedyL1Collapse(BidirCondCounts<String, String> nodeAligns)
	{
		int n = nodeAligns.NumSrcItems() + nodeAligns.NumTgtItems() - 2;
		RunGreedyL1Collapse(nodeAligns, n);
	}


	public static void
	RunFullGreedyKLCollapse(BidirCondCounts<String, String> nodeAligns)
	{
		int n = nodeAligns.NumSrcItems() + nodeAligns.NumTgtItems() - 2;
		RunGreedyKLCollapse(nodeAligns, n);
	}
	*/

	
	public static void
	RunGreedyL1Collapse(BidirCondCounts<String, String> nodeAligns, int numIters,
						boolean collapseSrc, boolean collapseTgt)
	{
		// Initialize data structures to hold collapsed-label mappings:
		Map<String, List<String>> srcCollapseMap =
			new HashMap<String, List<String>>();
		for(String srcLabel : nodeAligns.GetSrcItems())
		{
			List<String> l = new ArrayList<String>();
			l.add(srcLabel);
			srcCollapseMap.put(srcLabel, l);
		}
		Map<String, List<String>> tgtCollapseMap =
			new HashMap<String, List<String>>();
		for(String tgtLabel : nodeAligns.GetTgtItems())
		{
			List<String> l = new ArrayList<String>();
			l.add(tgtLabel);
			tgtCollapseMap.put(tgtLabel, l);
		}
		
		// Print distribution matrices before collapsing:
		//System.out.println("===== INITIAL ALIGNMENT DISTRIBUTIONS =====\n");
		//nodeAligns.PrintNormDistributionMatrix();
		//System.out.println("");
		
		// Run the specified number of collapsing iterations:
		for(int i = 1; i <= numIters; i++)
		{
			System.err.println("There are now " + nodeAligns.NumPairs() +
							   " joint labels: " + nodeAligns.NumSrcItems() +
							   " source and " + nodeAligns.NumTgtItems() +
							   " target.");
			//nodeAligns.PrintL1NearestNeighbors(true);  // new test line
			//System.err.println("Src nearest neighbor std dev: " +
			//				   L1NearestNeighborStdDev(nodeAligns, true));
			//System.err.println("Tgt nearest neighbor std dev: " +
			//				   L1NearestNeighborStdDev(nodeAligns, false));
	
			// Find the closest label pair on both source and target side:
			double sValue = Double.MAX_VALUE;
			String[] minS = new String[2];
			double tValue = Double.MAX_VALUE;
			String[] minT = new String[2];
			if(collapseSrc)
			{
				if(nodeAligns.GetSrcItems().size() >= 2)
				{
					minS = GetMinL1Dist(nodeAligns, true);
					sValue = nodeAligns.CalcTGSL1Dist(minS[0], minS[1]);
				}
			}
			if(collapseTgt)
			{
				if(nodeAligns.GetTgtItems().size() >= 2)
				{
					minT = GetMinL1Dist(nodeAligns, false);
					tValue = nodeAligns.CalcSGTL1Dist(minT[0], minT[1]);
				}
			}

			if(sValue == Double.MAX_VALUE && tValue == Double.MAX_VALUE)
				break;
			
			// Merge the pair that's closer:
			if(sValue < tValue)
			{
				// Update source-side map and merge labels:
				List<String> l = new ArrayList<String>();
				l.addAll(srcCollapseMap.remove(minS[0]));
				l.addAll(srcCollapseMap.remove(minS[1]));
				String newLabel = minS[0] + "|" + minS[1];
				srcCollapseMap.put(newLabel, l);
				nodeAligns.MergeSrcItems(minS[0], minS[1], newLabel);
			}
			else
			{
				// Update target-side map and merge labels:
				List<String> l = new ArrayList<String>();
				l.addAll(tgtCollapseMap.remove(minT[0]));
				l.addAll(tgtCollapseMap.remove(minT[1]));
				String newLabel = minT[0] + "|" + minT[1];
				tgtCollapseMap.put(newLabel, l);
				nodeAligns.MergeTgtItems(minT[0], minT[1], newLabel);
			}

			System.out.println("=== ITERATION " + i + " TABLES ===");
			System.out.println("=== SOURCE-SIDE COLLAPSING ===");
			PrintCollapseTable(srcCollapseMap);
			System.out.println("\n=== TARGET-SIDE COLLAPSING ===");
			PrintCollapseTable(tgtCollapseMap);
			
		}
		System.err.println("There are now " + nodeAligns.NumPairs() +
						   " joint labels: " + nodeAligns.NumSrcItems() +
						   " source and " + nodeAligns.NumTgtItems() +
				   		   " target.");
		
		// Print distribution matrices again after collapsing:
		//System.out.println("\n===== FINAL ALIGNMENT DISTRIBUTIONS =====\n");
		//nodeAligns.PrintNormDistributionMatrix();
		//System.out.println("");
		
		// Print out final merge tables:
		System.out.println("=== FINAL TABLES ===");
		System.out.println("=== SOURCE-SIDE COLLAPSING ===");
		PrintCollapseTable(srcCollapseMap);
		System.out.println("\n=== TARGET-SIDE COLLAPSING ===");
		PrintCollapseTable(tgtCollapseMap);
	}

	
	public static void
	RunGreedyKLCollapse(BidirCondCounts<String, String> nodeAligns, int numIters,
						boolean collapseSrc, boolean collapseTgt)
	{
		// Initialize data structures to hold collapsed-label mappings:
		Map<String, List<String>> srcCollapseMap =
			new HashMap<String, List<String>>();
		for(String srcLabel : nodeAligns.GetSrcItems())
		{
			List<String> l = new ArrayList<String>();
			l.add(srcLabel);
			srcCollapseMap.put(srcLabel, l);
		}
		Map<String, List<String>> tgtCollapseMap =
			new HashMap<String, List<String>>();
		for(String tgtLabel : nodeAligns.GetTgtItems())
		{
			List<String> l = new ArrayList<String>();
			l.add(tgtLabel);
			tgtCollapseMap.put(tgtLabel, l);
		}

		// Avoid probabilities of 0 when computing KL: add a pseudo-count
		// of 1 to every (src, tgt) pair:
		for(String src : nodeAligns.GetSrcItems())
			for(String tgt : nodeAligns.GetTgtItems())
				nodeAligns.AddCount(src, tgt, 1);
		
		// Run the specified number of collapsing iterations:
		for(int i = 1; i <= numIters; i++)
		{
			// Find the closest label pair on both source and target side:
			double sValue = Double.MAX_VALUE;
			String[] minS = new String[2];
			double tValue = Double.MAX_VALUE;
			String[] minT = new String[2];
			if(collapseSrc)
			{
				minS = GetMinKLDist(nodeAligns, true);
				sValue = nodeAligns.CalcTGSL1Dist(minS[0], minS[1]);
			}
			if(collapseTgt)
			{
				minT = GetMinKLDist(nodeAligns, false);
				tValue = nodeAligns.CalcSGTL1Dist(minT[0], minT[1]);
			}

			// Merge the pair that's closer:
			if(sValue < tValue)
			{
				// Update source-side map and merge labels:
				List<String> l = new ArrayList<String>();
				l.addAll(srcCollapseMap.remove(minS[0]));
				l.addAll(srcCollapseMap.remove(minS[1]));
				String newLabel = minS[0] + "|" + minS[1];
				srcCollapseMap.put(newLabel, l);
				nodeAligns.MergeSrcItems(minS[0], minS[1], newLabel);
			}
			else
			{
				// Update target-side map and merge labels:
				List<String> l = new ArrayList<String>();
				l.addAll(tgtCollapseMap.remove(minT[0]));
				l.addAll(tgtCollapseMap.remove(minT[1]));
				String newLabel = minT[0] + "|" + minT[1];
				tgtCollapseMap.put(newLabel, l);
				nodeAligns.MergeTgtItems(minT[0], minT[1], newLabel);
			}
		}
		
		// Print out final merge tables:
		System.out.println("=== SOURCE-SIDE COLLAPSING ===");
		PrintCollapseTable(srcCollapseMap);
		System.out.println("\n=== TARGET-SIDE COLLAPSING ===");
		PrintCollapseTable(tgtCollapseMap);
	}

	public static String getClosestLabelByL1(String label1, BidirCondCounts<String, String> nodeAligns, Set<String> labels, boolean srcSide)
	{
		String minLabel = "";
		double minDiff = 2.1;
		for(String label2 : labels)
		{
			if(label1.equals(label2))
				continue;

			double diff;
			if(srcSide)
				diff = nodeAligns.CalcTGSL1Dist(label1, label2);
			else
				diff = nodeAligns.CalcSGTL1Dist(label1, label2);

			if(diff < minDiff)
			{
				minLabel = label2;	
				minDiff = diff;
			}
		}
		return minLabel;
	}

	
	public static String[]
	GetMinL1Dist(BidirCondCounts<String, String> nodeAligns, boolean srcSide)
	{
		// Get list of all source (or target) labels:
		Set<String> labels = null;
		if(srcSide)
			labels = nodeAligns.GetSrcItems();
		else
			labels = nodeAligns.GetTgtItems();
	
		// Loop over all pairs of labels to find the two whose L1 distance
		// is the smallest:
		String[] minLabels = {"", ""};
		double minDiff = 2.1;  // Actual max value is 2.0

		/*
		ExecutorService es = Executors.newFixedThreadPool(2);
		List<Callable<Object>> todo = new ArrayList<Callable<Object>>(singleTable.size());

		for (DataTable singleTable : uniquePhrases)
		{ todo.add(Executors.callable(new ComputeDTask(singleTable))); }

		Future<Object> answers = es.invokeAll(todo);
		*/

		HashMap<String, Future<String>> tasks = new HashMap<String, Future<String>>();
		executor = Executors.newFixedThreadPool(numThreads);
		for(String label : labels)
		{
			ParallelTask task = new ParallelTask(label, nodeAligns, srcSide);
			Future<String> future = executor.submit(task);
			tasks.put(label, future);
		}

		for(String label1 : labels)
		{
			try
			{	
				Future<String> future = tasks.get(label1);
				String label2 = future.get();

				double diff;
				if(srcSide)
					diff = nodeAligns.CalcTGSL1Dist(label1, label2);
				else
					diff = nodeAligns.CalcSGTL1Dist(label1, label2);

				if(diff < minDiff)
				{
					minLabels[0] = label1;
					minLabels[1] = label2;
					minDiff = diff;	
				}
			}
			catch(InterruptedException e)
			{
				throw new RuntimeException(e);
			}
			catch(ExecutionException e)
			{
				throw new RuntimeException(e);
			}
		}
		executor.shutdown();
		//System.err.println("Closest labels are " + minLabels[0] + " and " +
		//		minLabels[1] + " with difference " + minDiff);
		return minLabels;
	}

	public static String[]
	GetMinKLDist(BidirCondCounts<String, String> nodeAligns, boolean srcSide)
	{
		// Get list of all source (or target) labels:
		Set<String> labels = null;
		if(srcSide)
			labels = nodeAligns.GetSrcItems();
		else
			labels = nodeAligns.GetTgtItems();

		// Loop over all pairs of labels to find the two whose KL divergence
		// is the smallest:
		String[] minLabels = {"", ""};
		double minDiff = Double.MAX_VALUE;

		String[] minLabelByLabel = new String[ labels.size() ];
		for(String label1 : labels)
		{
			for(String label2 : labels)
			{
				if(label1.equals(label2))
					continue;
				double diff = Double.MAX_VALUE;
				if(srcSide)
					diff = nodeAligns.CalcTGSKLDivergence(label1, label2);
				else
					diff = nodeAligns.CalcSGTKLDivergence(label1, label2);
				if(diff < minDiff)
				{
					minLabels[0] = label1;
					minLabels[1] = label2;
					minDiff = diff;
				}
			}
		}
		//System.err.println("Closest labels are " + minLabels[0] + " and " +
		//		minLabels[1] + " with difference " + minDiff);
		return minLabels;
	}
	
	
	public static void PrintCollapseTable(Map<String, List<String>> collapseMap)
	{
		// x
		for(String collapsed : collapseMap.keySet())
			for(String original : collapseMap.get(collapsed))
				System.out.println(original + "\t" + collapsed);
	}
	
	
	public static double L1NearestNeighborStdDev(BidirCondCounts<String, String> nodeAligns,
												 boolean srcSide)
	{
		// Get list of labels in use:
		Set<String> labels = null;
		if(srcSide)
			labels = nodeAligns.GetSrcItems();
		else
			labels = nodeAligns.GetTgtItems();
				
		// For each, get the distance to its closest neighbor; compute mean:
		double mean = 0.0;
		for(String label : labels)
		{
			BidirCondCounts<String, String>.IDDist<String> neighbor = null;
			if(srcSide)
				neighbor = nodeAligns.GetL1NearestSrcNeighbor(label);
			else
				neighbor = nodeAligns.GetL1NearestTgtNeighbor(label);
			mean += neighbor.GetDist();
		}
		mean = mean / (double)labels.size();
		
		// TEMP PRINT BLOCK:
		if(srcSide)
			System.err.println("Src mean: " + mean);
		else
			System.err.println("Tgt mean: " + mean);
				
		// Now compute sum of squared errors:
		double stdDev = 0.0;
		for(String label : labels)
		{
			BidirCondCounts<String, String>.IDDist<String> neighbor = null;
			if(srcSide)
				neighbor = nodeAligns.GetL1NearestSrcNeighbor(label);
			else
				neighbor = nodeAligns.GetL1NearestTgtNeighbor(label);
			stdDev += Math.pow(neighbor.GetDist() - mean, 2);
		}
		
		// Finally, compute standard deviation:
		stdDev = stdDev / (double)labels.size();
		stdDev = Math.sqrt(stdDev);
		return stdDev;
	}
}
