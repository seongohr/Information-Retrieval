import java.io.IOException;
import java.util.*;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class InvertedIndexBigrams {

  public static class TokenizerMapper extends Mapper<Object, Text, Text, Text>{

    private static final IntWritable one = new IntWritable(1);
    private Text word = new Text();
    private Text docID = new Text();
    private Text docContent = new Text();

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      String tokens = value.toString();
      int firstTabPos = tokens.indexOf("\t");
      docID.set(tokens.substring(0, firstTabPos));
      docContent.set(tokens.substring(firstTabPos +1).replaceAll("[^a-zA-Z]+"," ").toLowerCase().trim());
      
      StringTokenizer itr1 = new StringTokenizer(docContent.toString());
      StringTokenizer itr2 = new StringTokenizer(docContent.toString());
      itr2.nextToken();
      
      while (itr2.hasMoreTokens()) {
        word.set(itr1.nextToken() + " " + itr2.nextToken());
        context.write(word, docID);
      } 
    }    
  }     

public static class IntSumReducer extends Reducer<Text, Text, Text, Text>{
    private Text docId = new Text();
    private static final IntWritable one = new IntWritable(1);
    
    public void reduce(Text key, Iterable<Text> values,Context context) throws IOException, InterruptedException {
        HashMap<String,Integer> map = new HashMap<String,Integer>();
        
        for(Text val : values) {
            if(map.containsKey(val.toString())){
            
                map.put(val.toString(),map.get(val.toString()) + 1);
            }
            else {
                map.put(val.toString(),1);
            }
        }
        String outputFormat = map.toString();
        
        outputFormat = outputFormat.replace("{", "");
        outputFormat = outputFormat.replace("}", "");
        outputFormat = outputFormat.replace("=", ":");
        outputFormat = outputFormat.replace(",", "");
        context.write(key, new Text(outputFormat));
    }
  }
public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(InvertedIndexBigrams.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setReducerClass(IntSumReducer.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(Text.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}