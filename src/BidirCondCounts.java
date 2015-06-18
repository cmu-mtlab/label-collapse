import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


// BidirCondCounts class:
//    General-purpose class for keeping track of source-given-target and/or
//    target-given-source probabilities from some set of joint data.  SrcT is
//    the data type of the source-side observables, and TgtT is the data type
//    of the target-side observables.
public class BidirCondCounts<SrcT, TgtT>
{
	// Constructor:
	public BidirCondCounts(boolean storeSGT, boolean storeTGS)
	{
		// Initialize member variables:
		sgtCounts = new HashMap<TgtT, Map<SrcT, Integer>>();
		tgsCounts = new HashMap<SrcT, Map<TgtT, Integer>>();
		sgtMarginals = new HashMap<TgtT, Integer>();
		tgsMarginals = new HashMap<SrcT, Integer>();

		// Keep track of which ones we'll fill in:
		this.storeSGT = storeSGT;
		this.storeTGS = storeTGS;
	}


	// AddCount function:
	//    Add a joint count to the current data totals.
	public void AddCount(SrcT srcID, TgtT tgtID, int count)
	{
		// Update source-given-target marginal and distribution counts:
		if(storeSGT)
		{
			if(sgtMarginals.containsKey(tgtID))
			{
				sgtMarginals.put(tgtID, sgtMarginals.get(tgtID) + count);
				if(sgtCounts.get(tgtID).containsKey(srcID))
					sgtCounts.get(tgtID).put(srcID,
											 sgtCounts.get(tgtID).get(srcID) + count);
				else
					sgtCounts.get(tgtID).put(srcID, count);
			}
			else
			{
				sgtMarginals.put(tgtID, count);
				sgtCounts.put(tgtID, new HashMap<SrcT, Integer>());
				sgtCounts.get(tgtID).put(srcID, count);
			}
		}
			
		// Update target-given-source marginal and distribution counts:
		if(storeTGS)
		{
			if(tgsMarginals.containsKey(srcID))
			{
				tgsMarginals.put(srcID, tgsMarginals.get(srcID) + count);
				if(tgsCounts.get(srcID).containsKey(tgtID))
					tgsCounts.get(srcID).put(tgtID,
											 tgsCounts.get(srcID).get(tgtID) + count);
				else
					tgsCounts.get(srcID).put(tgtID, count);
			}
			else
			{
				tgsMarginals.put(srcID, count);
				tgsCounts.put(srcID, new HashMap<TgtT, Integer>());
				tgsCounts.get(srcID).put(tgtID, count);
			}
		}
	}


	// NumPairs function:
	//    Returns the total number of unique (src, tgt) pairs currently stored.
	//    Either SGT or TGS counts (or both) may be filled in; if they're
	//    both filled in, we assume the total count is the same and just use
	//    the SGT side.
	public int NumPairs()
	{
		// Compute number of data points in SGT table:
		int total1 = 0;
		for(TgtT id : sgtCounts.keySet())
			total1 += sgtCounts.get(id).size();
		if(total1 > 0)
			return total1;

		// If nothing was in SGT, compute TGS and return that instead:
		int total2 = 0;
		for(SrcT id : tgsCounts.keySet())
			total2 += tgsCounts.get(id).size();
		return total2;
	}

	
	public int NumSrcItems()
	{
		return tgsMarginals.size();
	}
	
	public int NumTgtItems()
	{
		return sgtMarginals.size();
	}
	

	public int NumTotalSrcCounts()
	{
		int total = 0;
		for(SrcT id : tgsMarginals.keySet())
			total += tgsMarginals.get(id);
		return total;
	}
	
	public int NumTotalTgtCounts()
	{
		int total = 0;
		for(TgtT id : sgtMarginals.keySet())
			total += sgtMarginals.get(id);
		return total;
	}


	public int NumSGTAlternatives(TgtT tgtID)
	{
		if(!sgtCounts.containsKey(tgtID))
			return 0;
		return sgtCounts.get(tgtID).size();
	}
	
	public int NumTGSAlternatives(SrcT srcID)
	{
		if(!tgsCounts.containsKey(srcID))
			return 0;
		return tgsCounts.get(srcID).size();
	}


	public Map<SrcT, Integer> GetSGTAlternativeCounts(TgtT tgtID)
	{
		if(!sgtCounts.containsKey(tgtID))
			return new HashMap<SrcT, Integer>();
		return sgtCounts.get(tgtID);
	}

	public Map<TgtT, Integer> GetTGSAlternativeCounts(SrcT srcID)
	{
		if(!tgsCounts.containsKey(srcID))
			return new HashMap<TgtT, Integer>();
		return tgsCounts.get(srcID);
	}


	public Set<SrcT> GetSrcItems()
	{
		return tgsMarginals.keySet();
	}

	public Set<TgtT> GetTgtItems()
	{
		return sgtMarginals.keySet();
	}

	
	public void MergeSrcItems(SrcT srcID1, SrcT srcID2, SrcT mergedID)
	{
		// Print merging information in Graphviz format:
		double diff = CalcTGSL1Dist(srcID1, srcID2);
		System.err.format("\t\"Src:%s\" -> \"Src:%s\" [label = \"%.4f\"];\n",
				          mergedID.toString(), srcID1.toString(), diff);
		System.err.format("\t\"Src:%s\" -> \"Src:%s\" [label = \"%.4f\"];\n",
						  mergedID.toString(), srcID2.toString(), diff);

		// Merge TGS marginal counts:
		int tgsMarg = tgsMarginals.remove(srcID1) +
			          tgsMarginals.remove(srcID2);
		tgsMarginals.put(mergedID, tgsMarg);

		// Merge TGS distributional counts: start with ID1's, then add in
		// everything from ID2:
		Map<TgtT, Integer> newTGSs =
			new HashMap<TgtT, Integer>(tgsCounts.remove(srcID1));
		Map<TgtT, Integer> id2TGSs =
			new HashMap<TgtT, Integer>(tgsCounts.remove(srcID2));
		for(TgtT tgt : id2TGSs.keySet())
		{
			if(newTGSs.containsKey(tgt))
			{
				int count = newTGSs.get(tgt) + id2TGSs.get(tgt);
				newTGSs.put(tgt, count);
			}
			else
				newTGSs.put(tgt, id2TGSs.get(tgt));
		}
		tgsCounts.put(mergedID, newTGSs);
		
		// Merge SGT distributional counts: for each target, take out ID1 and
		// ID2 counts, then put back their sum:
		for(TgtT tgt : sgtCounts.keySet())
		{
			int count = 0;
			if(sgtCounts.get(tgt).containsKey(srcID1))
				count += sgtCounts.get(tgt).remove(srcID1);
			if(sgtCounts.get(tgt).containsKey(srcID2))
				count += sgtCounts.get(tgt).remove(srcID2);
			if(count > 0)
				sgtCounts.get(tgt).put(mergedID, count);
		}
	}
	
	
	public void MergeTgtItems(TgtT tgtID1, TgtT tgtID2, TgtT mergedID)
	{
		// Print merging information in Graphviz format:
		double diff = CalcSGTL1Dist(tgtID1, tgtID2);
		System.err.format("\t\"Tgt:%s\" -> \"Tgt:%s\" [label = \"%.4f\"];\n",
		          		  mergedID.toString(), tgtID1.toString(), diff);
		System.err.format("\t\"Tgt:%s\" -> \"Tgt:%s\" [label = \"%.4f\"];\n",
		          		  mergedID.toString(), tgtID2.toString(), diff);

		// Merge SGT marginal counts:
		int sgtMarg = sgtMarginals.remove(tgtID1) +
			          sgtMarginals.remove(tgtID2);
		sgtMarginals.put(mergedID, sgtMarg);
		
		// Merge SGT distributional counts: start with ID1's, then add in
		// everything from ID2:
		Map<SrcT, Integer> newSGTs =
			new HashMap<SrcT, Integer>(sgtCounts.remove(tgtID1));
		Map<SrcT, Integer> id2SGTs =
			new HashMap<SrcT, Integer>(sgtCounts.remove(tgtID2));
		for(SrcT src : id2SGTs.keySet())
		{
			if(newSGTs.containsKey(src))
			{
				int count = newSGTs.get(src) + id2SGTs.get(src);
				newSGTs.put(src, count);
			}
			else
				newSGTs.put(src, id2SGTs.get(src));
		}
		sgtCounts.put(mergedID, newSGTs);
		
		// Merge TGS distributional counts: for each source, take out ID1 and
		// ID2 counts, then put back their sum:
		for(SrcT src : tgsCounts.keySet())
		{
			int count = 0;
			if(tgsCounts.get(src).containsKey(tgtID1))
				count += tgsCounts.get(src).remove(tgtID1);
			if(tgsCounts.get(src).containsKey(tgtID2))
				count += tgsCounts.get(src).remove(tgtID2);
			if(count > 0)
				tgsCounts.get(src).put(mergedID, count);
		}		
	}
	

	public double CalcSrcProb(SrcT srcID)
	{
		if(!tgsMarginals.containsKey(srcID))
			return 0.0;
		
		int total = 0;
		for(SrcT id : tgsMarginals.keySet())
			total += tgsMarginals.get(id);
		return ((double)tgsMarginals.get(srcID) / (double)total);
	}

	public double CalcTgtProb(TgtT tgtID)
	{
		if(!sgtMarginals.containsKey(tgtID))
			return 0.0;
		
		int total = 0;
		for(TgtT id : sgtMarginals.keySet())
			total += sgtMarginals.get(id);
		return ((double)sgtMarginals.get(tgtID) / (double)total);
	}


	public int SrcCount(SrcT srcID)
	{
		if(!tgsMarginals.containsKey(srcID))
			return 0;
		return tgsMarginals.get(srcID);
	}

	public int TgtCount(TgtT tgtID)
	{
		if(!sgtMarginals.containsKey(tgtID))
			return 0;
		return sgtMarginals.get(tgtID);
	}

	
	public double CalcSGTProb(SrcT srcID, TgtT tgtID)
	{
		// Zero probability if the source or target IDs don't exist:
		if(!sgtMarginals.containsKey(tgtID))
			return 0.0;
		if(!sgtCounts.get(tgtID).containsKey(srcID))
			return 0.0;
		
		// Otherwise simple division:
		return ((double)sgtCounts.get(tgtID).get(srcID) /
				(double)sgtMarginals.get(tgtID));
	}

	public double CalcTGSProb(SrcT srcID, TgtT tgtID)
	{
		// Zero probability if the source or target IDs don't exist:
		if(!tgsMarginals.containsKey(srcID))
			return 0.0;
		if(!tgsCounts.get(srcID).containsKey(tgtID))
			return 0.0;

		// Otherwise simple division:
		return ((double)tgsCounts.get(srcID).get(tgtID) /
				(double)tgsMarginals.get(srcID));
	}


	public double CalcSGTEntropy(TgtT tgtID)
	{
		// Zero entropy if target ID doesn't exist:
		if(!sgtMarginals.containsKey(tgtID))
			return 0.0;
		
		// Otherwise compute entropy of the target ID's various sources:
		double entropy = 0.0;
		for(SrcT srcID : sgtCounts.get(tgtID).keySet())
		{
			double p = CalcSGTProb(srcID, tgtID);
			entropy -= (p * Math.log(p) / Math.log(2));
		}
		return entropy;
	}

	public double CalcTGSEntropy(SrcT srcID)
	{
		// Zero entropy if source ID doesn't exist:
		if(!tgsMarginals.containsKey(srcID))
			return 0.0;
		
		// Otherwise compute entropy of the source ID's various targets:
		double entropy = 0.0;
		for(TgtT tgtID : tgsCounts.get(srcID).keySet())
		{
			double p = CalcTGSProb(srcID, tgtID);
			entropy -= (p * Math.log(p) / Math.log(2));
		}
		return entropy;
	}

	
	// NOTE: If you already have the actual entropy, this function may be a
	// waste of time because it will re-compute it.
	public double CalcSGTEntropyReduction(TgtT tgtID)
	{
		// If all source alternatives were equally likely, the entropy
		// would be lg(#alts):
		double expect = Math.log(NumSGTAlternatives(tgtID)) / Math.log(2);
		double actual = CalcSGTEntropy(tgtID);
		return (expect - actual);
	}

	// NOTE: If you already have the actual entropy, this function may be a
	// waste of time because it will re-compute it.
	public double CalcTGSEntropyReduction(SrcT srcID)
	{
		// If all target alternatives were equally likely, the entropy
		// would be lg(#alts):
		double expect = Math.log(NumTGSAlternatives(srcID)) / Math.log(2);
		double actual = CalcTGSEntropy(srcID);
		return (expect - actual);
	}


	public Map<SrcT, Double> GetNormSGTDist(TgtT tgtID)
	{
		// Convert the raw distribution counts into probabilities:
		Map<SrcT, Double> probs = new HashMap<SrcT, Double>();
		Map<SrcT, Integer> counts = sgtCounts.get(tgtID);
		for(SrcT srcID : counts.keySet())
		{
			probs.put(srcID,
					  (double)counts.get(srcID) / (double)sgtMarginals.get(tgtID));
		}
		return probs;
	}

	public Map<TgtT, Double> GetNormTGSDist(SrcT srcID)
	{
		// Convert the raw distribution counts into probabilities:
		Map<TgtT, Double> probs = new HashMap<TgtT, Double>();
		Map<TgtT, Integer> counts = tgsCounts.get(srcID);
		for(TgtT tgtID : counts.keySet())
		{
			probs.put(tgtID,
					  (double)counts.get(tgtID) / (double)tgsMarginals.get(srcID));
		}
		return probs;
	}

	
	// CalcSGTL1Dist function:
	//    Compute the L1 distance between the distributions for the two
	//    specified target IDs.  This is the sum of the absolute differences
	//    in probability value for each source ID.
	public double CalcSGTL1Dist(TgtT tgtID1, TgtT tgtID2)
	{
		Map<SrcT, Double> dist1 = GetNormSGTDist(tgtID1);
		Map<SrcT, Double> dist2 = GetNormSGTDist(tgtID2);
		double totalDiff = 0.0;
		
		// Add up the difference in probabilities between dist1 and dist2:
		for(SrcT srcID : dist1.keySet())
		{
			if(dist2.containsKey(srcID))
			{
				totalDiff += Math.abs(dist1.get(srcID) - dist2.get(srcID));
				dist2.remove(srcID);
			}
			else
				totalDiff += dist1.get(srcID);
		}
		
		// Anything left in dist2 hasn't been counted yet (and isn't in dist1):
		for(SrcT srcID : dist2.keySet())
		{
			totalDiff += dist2.get(srcID);
		}
		
		return totalDiff;
	}

	public double CalcTGSL1Dist(SrcT srcID1, SrcT srcID2)
	{
		Map<TgtT, Double> dist1 = GetNormTGSDist(srcID1);
		Map<TgtT, Double> dist2 = GetNormTGSDist(srcID2);
		double totalDiff = 0.0;
		
		// Add up the difference in probabilities between dist1 and dist2:
		for(TgtT tgtID : dist1.keySet())
		{
			if(dist2.containsKey(tgtID))
			{
				totalDiff += Math.abs(dist1.get(tgtID) - dist2.get(tgtID));
				dist2.remove(tgtID);
			}
			else
				totalDiff += dist1.get(tgtID);
		}
		
		// Anything left in dist2 hasn't been counted yet (and isn't in dist1):
		for(TgtT tgtID : dist2.keySet())
		{
			totalDiff += dist2.get(tgtID);
		}
		
		return totalDiff;
	}


	// CalcSGTKLDivergence function:
	//    Compute the KL divergence between the distributions for the two
	//    specified target IDs.  This is the symmetrized divergence
	//    KL(tgtID1 || tgtID2) + KL(tgtID2 || tgtID1),
	//    as from http://en.wikipedia.org/wiki/KL_divergence
	public double CalcSGTKLDivergence(TgtT tgtID1, TgtT tgtID2)
	{
		Map<SrcT, Double> dist1 = GetNormSGTDist(tgtID1);
		Map<SrcT, Double> dist2 = GetNormSGTDist(tgtID2);
		double diff1 = 0.0;
		double diff2 = 0.0;
		
		// Add up the KL divergence between dist1 and dist2:
		for(SrcT srcID : dist1.keySet())
		{
			if(dist1.containsKey(srcID) && dist2.containsKey(srcID))
			{
				// Both target IDs were seen with this source ID;
				// we're treating 0 log(0) and p log(\infty) both as 0:
				diff1 += (dist1.get(srcID) *
						 Math.log(dist1.get(srcID) / dist2.get(srcID)) /
						 Math.log(2.0));
				diff2 += (dist2.get(srcID) *
						 Math.log(dist2.get(srcID) / dist1.get(srcID)) /
						 Math.log(2.0));
			}
		}
		return (diff1 + diff2);
	}

	public double CalcTGSKLDivergence(SrcT srcID1, SrcT srcID2)
	{
		Map<TgtT, Double> dist1 = GetNormTGSDist(srcID1);
		Map<TgtT, Double> dist2 = GetNormTGSDist(srcID2);
		double diff1 = 0.0;
		double diff2 = 0.0;
		
		// Add up the KL divergence between dist1 and dist2:
		for(TgtT tgtID : dist1.keySet())
		{
			if(dist1.containsKey(tgtID) && dist2.containsKey(tgtID))
			{
				// Both source IDs were seen with this target ID;
				// we're treating 0 log(0) and p log(\infty) both as 0:
				diff1 += (dist1.get(tgtID) *
						 Math.log(dist1.get(tgtID) / dist2.get(tgtID)) /
						 Math.log(2));
				diff2 += (dist2.get(tgtID) *
						 Math.log(dist2.get(tgtID) / dist1.get(tgtID)) /
						 Math.log(2));
			}
		}
		return (diff1 + diff2);
	}

	
	// GetL1NearestSrcNeighbor function:
	//    Given a source ID, finds the source ID with the smallest L1
	//    distance to it.  Returns a pair of the ID and its distance.
	public IDDist<SrcT> GetL1NearestSrcNeighbor(SrcT srcID)
	{
		// Look over all other source IDs and find the min L1 distance:
		SrcT minSrc = null;
		double minDist = 2.1;   // actual max is 2.0
		for(SrcT otherSrc : tgsCounts.keySet())
		{
			if(srcID.equals(otherSrc))
				continue;
			double dist = CalcTGSL1Dist(srcID, otherSrc);
			if(dist < minDist)
			{
				minSrc = otherSrc;
				minDist = dist;
			}
		}
		
		// Return the closest source ID:
		return new IDDist<SrcT>(minSrc, minDist);
	}

	// GetL1NearestTgtNeighbor function:
	//    Given a target ID, finds the target ID with the smallest L1
	//    distance to it.  Returns a pair of the ID and its distance.
	public IDDist<TgtT> GetL1NearestTgtNeighbor(TgtT tgtID)
	{
		// Look over all other target IDs and find the min L1 distance:
		TgtT minTgt = null;
		double minDist = 2.1;   // actual max is 2.0
		for(TgtT otherTgt : sgtCounts.keySet())
		{
			if(tgtID.equals(otherTgt))
				continue;
			double dist = CalcSGTL1Dist(tgtID, otherTgt);
			if(dist < minDist)
			{
				minTgt = otherTgt;
				minDist = dist;
			}
		}
		
		// Return the closest target ID:
		return new IDDist<TgtT>(minTgt, minDist);
	}

	
	public void PrintL1NearestNeighbors(boolean distsOnly)
	{
		// Print nearest neighbor for each source ID (all on same line):
		System.out.print("Source L1 nearest neighbors:");
		SortedSet<SrcT> srcKeys = new TreeSet<SrcT>(tgsCounts.keySet());
		for(SrcT src : srcKeys)
		{
			IDDist<SrcT> neighbor = GetL1NearestSrcNeighbor(src);
			if(distsOnly)
				System.out.print("\t" + neighbor.GetDist());
			else
			{
				System.out.print("\t" + src.toString() + " " +
								 neighbor.GetID().toString() + " " +
								 neighbor.GetDist());
			}
		}
		System.out.print("\n");
		
		// Print nearest neighbor for each target ID (all on same line):
		System.out.print("Target L1 nearest neighbors:");
		SortedSet<TgtT> tgtKeys = new TreeSet<TgtT>(sgtCounts.keySet());
		for(TgtT tgt : tgtKeys)
		{
			IDDist<TgtT> neighbor = GetL1NearestTgtNeighbor(tgt);
			if(distsOnly)
				System.out.print("\t" + neighbor.GetDist());
			else
			{
				System.out.print("\t" + tgt.toString() + " " +
								 neighbor.GetID().toString() + " " +
								 neighbor.GetDist());
			}
		}
		System.out.print("\n");
	}
	
	
	public void PrintNormDistributionMatrix()
	{
		System.out.println("===== TARGET GIVEN SOURCE =====\n");
		
		// Header row:
		SortedSet<TgtT> tgtKeys = new TreeSet<TgtT>(sgtCounts.keySet());
		System.out.print("\t");
		for(TgtT tgt : tgtKeys)
			System.out.print(tgt.toString() + "\t");
		System.out.print("\n");
		
		// Main table:
		SortedSet<SrcT> srcKeys = new TreeSet<SrcT>(tgsCounts.keySet());
		for(SrcT src : srcKeys)
		{
			System.out.print(src.toString() + "\t");
			for(TgtT tgt : tgtKeys)
				System.out.print(CalcTGSProb(src, tgt) + "\t");
			System.out.print("n=" + tgsMarginals.get(src) + "\n");
		}
		
		System.out.println("\n===== SOURCE GIVEN TARGET =====\n");
		
		// Header row:
		SortedSet<SrcT> innerKeys = new TreeSet<SrcT>(tgsCounts.keySet());
		System.out.print("\t");
		for(SrcT src : innerKeys)
			System.out.print(src.toString() + "\t");
		System.out.print("\n");
		
		// Main table:
		SortedSet<TgtT> outerKeys = new TreeSet<TgtT>(sgtCounts.keySet());
		for(TgtT tgt : outerKeys)
		{
			System.out.print(tgt + "\t");
			for(SrcT src : innerKeys)
				System.out.print(CalcSGTProb(src, tgt) + "\t");
			System.out.print("n=" + sgtMarginals.get(tgt) + "\n");
		}
	}

	
	// Inner class for an ID and a distance:
	public class IDDist<T>
	{
		public IDDist(T id, double dist)
		{
			this.id = id;
			this.dist = dist;
		}
		
		public T GetID()
		{
			return id;
		}
		
		public double GetDist()
		{
			return dist;
		}
		
		private T id;
		private double dist;
	}
	
	
	// Member variables:

	private boolean storeSGT;
	private boolean storeTGS;
	// SGT: Outer key is target; inner key is source:
	private Map<TgtT, Map<SrcT, Integer>> sgtCounts;
	private Map<TgtT, Integer> sgtMarginals;
	// TGS: Outer key is source; inner key is target:
	private Map<SrcT, Map<TgtT, Integer>> tgsCounts;
	private Map<SrcT, Integer> tgsMarginals;
}
