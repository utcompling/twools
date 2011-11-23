/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fogbow.textgrounder;

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.{IntWritable, NullWritable, Text}
import org.apache.hadoop.mapreduce.{Job, Mapper, Reducer}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import org.apache.hadoop.util.GenericOptionsParser
import scala.collection.JavaConversions._

class JsonTweetMapper extends Mapper[Object, Text, Text, NullWritable] {
  val data = new Text

  override 
  def map (key: Object, value: Text, context: Mapper[Object, Text, Text, NullWritable]#Context) {
	
	try {
	    val status = Status.fromJson(value.toString)
	    if (status.hasGeo()) {

		  data.set(List(
		    status.getUserId(),
		    status.getCreatedAt(),
		    status.getCoord(),
		    status.getLat(),
		    status.getLon(),
		    status.getText()
		  ) mkString "\t")
	
		  context.write(data, NullWritable.get())
	    }
	} catch {
	  case e: Exception => { 
		System.err.println("Bad JSON?") 
		System.err.println(value.toString)
		}
	}
  }
}

class IdentityReducer extends Reducer[Text,NullWritable,Text,NullWritable] {

 override 
 def reduce (key: Text, values: java.lang.Iterable[NullWritable], 
 			context: Reducer[Text,NullWritable,Text,NullWritable]#Context) {
	context write(key, NullWritable.get())
  }
}

object PreprocessJsonTweets {
  def main (args: Array[String]) {
    if (args.length != 3) {
      println("Usage: <inputPath> <outputPath> <numReduceTasks>")
      System.exit(1)
    }

    val conf = new Configuration()
    val job = new Job(conf, "")
   
    val outputPath = new Path(args(1))
    FileInputFormat.addInputPath(job, new Path(args(0)))
    FileOutputFormat.setOutputPath(job, outputPath)
    FileSystem.get(conf).delete(outputPath, true)
    job.setNumReduceTasks(args(2).toInt)

    job.setJarByClass(classOf[JsonTweetMapper])
    job.setMapperClass(classOf[JsonTweetMapper])
    job.setOutputKeyClass(classOf[Text])
    job.setOutputValueClass(classOf[NullWritable])
    //job.setReducerClass(classOf[IdentityReducer])

    System.exit(if (job.waitForCompletion(true)) 0 else 1)
  }
}
