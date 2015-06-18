import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class InputReader
{
	// FillFromScorableRuleFile function:
	//    Adds to the given distribution tables the counts of labels
	//    extracted from the left-hand sides of entries in the given Jon
	//    "ScorableRule" file.  The entries in the file may be lexical items,
	//    multi-word phrase pairs, or grammar rules, as below.
	// Format examples:
	//    L ||| [N::NN] ||| Reprise ||| Resumption |||  ||| 0-0 ||| 1.0
	//    P ||| [NP::NP] ||| Reprise ||| Resumption |||  ||| 0-0 ||| 1.0
    //    G ||| [NP::NP] ||| [N::NN,1] ||| [N::NN,1] |||  ||| 0-0 ||| 1.0
	public static void
	FillFromScorableRuleFile(BidirCondCounts<String, String> dataTables,
							 String srFile, boolean includeLex,
							 boolean includePhr, boolean includeGra)
	throws FileNotFoundException
	{
		// Open file:
		BufferedReader srReader =
			new BufferedReader(new FileReader(srFile));
		
		// Loop over entries in the ScorableRule file:
		System.err.println("Reading ScorableRule file...");
		int lineNum = 1;
		String rule;
		try
		{
			rule = srReader.readLine();
		}
		catch(IOException e)
		{
			System.err.println("Error reading from input file : l." +
					lineNum + " !");
			return;
		}
		
		while(rule != null)
		{
			// Break the ScorableRule line into columns:
			rule = rule.trim();
			String[] columns = rule.split(" \\|\\|\\| ");
			
			// Stop now if the rule is of a type we're not including:
			if((columns[0].equals("L") && !includeLex) ||
			   (columns[0].equals("P") && !includePhr) ||
               (columns[0].equals("G") && !includeGra))
			{
				// Nothing -- just read in next line at the end.
			}
			
			// Otherwise, process the line:
			else
			{
				// Have to parse out the LHS column into src and tgt side:
				String srcLHS = "";
				String tgtLHS = "";
				if(columns[1].contains("::::"))
				{
					srcLHS = ":";
                    tgtLHS = ":";
				}
				else if(columns[1].startsWith("[:::"))
				{
					srcLHS = ":";
					tgtLHS = columns[1].substring(4, columns[1].indexOf("]"));
				}
				else if(columns[1].endsWith(":::]"))
				{
					srcLHS = columns[1].substring(1, columns[1].indexOf(":"));
					tgtLHS = ":";
				}
				else
				{
					String[] lhsFields = columns[1].split("]|\\[|(::)");
					srcLHS = lhsFields[1];
					tgtLHS = lhsFields[2];
				}

				// Add alignment counts to stored distribution:
				if(!srcLHS.equals("") && !tgtLHS.equals(""))
				dataTables.AddCount(srcLHS, tgtLHS, 1);
			}
			
			// Read next line:
			try
			{
				rule = srReader.readLine();
			}
			catch(IOException e)
			{
				System.err.println("Error reading from input file!");
				return;
			}
			
			// Print periodic progress message:
			lineNum++;
			if(lineNum % 1000000 == 0)
				System.err.println("Read " + lineNum + " lines");
		}
		
		// Done; close all files:
		try
		{
			srReader.close();
		}
		catch(IOException e)
		{
			System.err.println("Error closing input file.");
			return;
		}
	}

	
	// FillFromOnelineFile function:
	//    Adds to the given distribution tables the counts of labels
	//    extracted from the left-hand sides of entries in the given Vamshi
	//    "oneline" file.  The entries in the file may be lexical items,
	//    multi-word phrase pairs, or grammar rules.
	// Format examples (columns separated by tabs):
	//    NNS  N     "people"         "citoyens"      0-0
	//    NP   NP    "the" "session"  "la" "session"  0-0 1-1
	//    VP   VP-P  ["to" VP ]       [VP-P ]         {2-1}
	//    VP   VP-P  ["to" VP ]       [VP-P ]         {2-1}    ||| [to materialise]
	public static void
	FillFromOnelineFile(BidirCondCounts<String, String> dataTables,
						String onelineFile)
	throws FileNotFoundException
	{
		// Open file:
		BufferedReader onelineReader =
			new BufferedReader(new FileReader(onelineFile));
		
		// Loop over entries in the oneline file:
		System.err.println("Reading oneline file...");
		int lineNum = 1;
		String oneline;
		try
		{
			oneline = onelineReader.readLine();
		}
		catch(IOException e)
		{
			System.err.println("Error reading from input file : l." +
					lineNum + " !");
			return;
		}
		
		while(oneline != null)
		{
			// Parse the line to get the source and target LHS labels:
			oneline = oneline.trim();
			String[] columns = oneline.split("\\t");
			
			// Add alignment counts to stored distribution:
			if(!columns[0].equals("") && !columns[1].equals(""))
				dataTables.AddCount(columns[0], columns[1], 1);
			
			// Read next line:
			try
			{
				oneline = onelineReader.readLine();
			}
			catch(IOException e)
			{
				System.err.println("Error reading from input file!");
				return;
			}
			lineNum++;
		}
		
		// Done; close all files:
		try
		{
			onelineReader.close();
		}
		catch(IOException e)
		{
			System.err.println("Error closing input file.");
			return;
		}
	}

	
	// FillFromCountsFile function:
	//    Adds to the given distribution tables the counts of label
	//    alignments in the given file of counts.  The counts file uses a
	//    simple format for general-purpose input.
	// Format examples (columns separated by tabs):
	//    NNS  N     3
	//    NP   NP    17
	//    VP   VP-P  5
	//    VP   VP-P  1
	public static void
	FillFromCountsFile(BidirCondCounts<String, String> dataTables,
					   String countsFile)
	throws FileNotFoundException
	{
		// Open file:
		BufferedReader lineReader =
			new BufferedReader(new FileReader(countsFile));
		
		// Loop over entries in the counts file:
		System.err.println("Reading counts file...");
		int lineNum = 1;
		String line;
		try
		{
			line = lineReader.readLine();
		}
		catch(IOException e)
		{
			System.err.println("Error reading from input file : l." +
					lineNum + " !");
			return;
		}
		
		while(line != null)
		{
			// Parse the line to get the source and target LHS labels:
			line = line.trim();
			String[] columns = line.split("\\t");
			
			// Add alignment counts to stored distribution:
			if(!columns[0].equals("") && !columns[1].equals("") &&
			   !columns[2].equals(""))
			{
				dataTables.AddCount(columns[0], columns[1],
							 		Integer.parseInt(columns[2]));
			}
			
			// Read next line:
			try
			{
				line = lineReader.readLine();
			}
			catch(IOException e)
			{
				System.err.println("Error reading from input file!");
				return;
			}
			lineNum++;
			
			// Print periodic progress message:
			lineNum++;
			if(lineNum % 10000 == 0)
				System.err.println("Read " + lineNum + " lines");
		}
		
		// Done; close all files:
		try
		{
			lineReader.close();
		}
		catch(IOException e)
		{
			System.err.println("Error closing input file.");
			return;
		}
	}


	// FillFromMosesFiles function:
	//    Adds to the given distribution tables the POS alignments specified
	//    by a Moses alignment file and source and target parse trees.
	// WARNING: No non-terminal node alignments are included; just word-to-word
	//    pre-terminals based on single word alignment links!
	public static void
	FillFromMosesFiles(BidirCondCounts<String, String> dataTables,
					   String srcParseFile, String tgtParseFile,
					   String mosesAlignsFile)
	throws FileNotFoundException
	{
		// Pre-compiled reg ex to match a word in a parse tree and its part
		// of speech, for example "(ADV simpler)":
		Pattern posWord =
			Pattern.compile("\\(([^\\(\\)]+) ([^\\(\\)]+)\\)");

		// Open files:
		BufferedReader srcReader =
			new BufferedReader(new FileReader(srcParseFile));
		BufferedReader tgtReader =
			new BufferedReader(new FileReader(tgtParseFile));
		BufferedReader alignReader =
			new BufferedReader(new FileReader(mosesAlignsFile));
		
		// Loop over sentences:
		System.err.println("Reading trees and Moses files...");
		int sentNum = 1;
		String srcLine, tgtLine, alignLine;
		try
		{
			srcLine = srcReader.readLine();
			tgtLine = tgtReader.readLine();
			alignLine = alignReader.readLine();
		}
		catch(IOException e)
		{
			System.err.println("Error reading from input file : S." +
					sentNum + " !");
			return;
		}
		
		while((srcLine != null) && (tgtLine != null) && (alignLine != null))
		{
			// Get a part of speech for each word in the parsed sentences:
			List<String> srcPOSSeq = new ArrayList<String>();
			Matcher m = posWord.matcher(srcLine);
			while(m.find())
				srcPOSSeq.add(m.group(1));
			List<String> tgtPOSSeq = new ArrayList<String>();
			m = posWord.matcher(tgtLine);
			while(m.find())
				tgtPOSSeq.add(m.group(1));
			
			// Extract source-to-target alignments:
			String[] aligns = alignLine.split("\\s+");
			for(String al : aligns)
			{
				// Each (zero-based) alignment has the form "6-9":
				String[] indexes = al.split("-");
				int srcInd = -1;
				int tgtInd = -1;
				try
				{
					srcInd = Integer.parseInt(indexes[0]);
					tgtInd = Integer.parseInt(indexes[1]);
				}
				catch(NumberFormatException e)
				{
					System.err.println("Malformatted alignment [" + alignLine +
							"] : S." + sentNum + " !");
					continue;
				}
				if((srcInd >= srcPOSSeq.size()) || (tgtInd >= tgtPOSSeq.size()) ||
				   (srcInd < 0) || (tgtInd < 0))
				{
					System.err.println("Alignment out of bounds [" + alignLine +
							"] : S." + sentNum + " !");
				}
				else
				{
					// Add alignment counts to stored distribution:
					dataTables.AddCount(srcPOSSeq.get(srcInd),
										tgtPOSSeq.get(tgtInd), 1);
				}
			}
			
			// Read next lines:
			try
			{
				srcLine = srcReader.readLine();
				tgtLine = tgtReader.readLine();
				alignLine = alignReader.readLine();
			}
			catch(IOException e)
			{
				System.err.println("Error reading from input file!");
				return;
			}
			sentNum++;
		}
		
		// Done; close all files:
		try
		{
			srcReader.close();
			tgtReader.close();
			alignReader.close();
		}
		catch(IOException e)
		{
			System.err.println("Error closing input files.");
			return;
		}
	}

}
