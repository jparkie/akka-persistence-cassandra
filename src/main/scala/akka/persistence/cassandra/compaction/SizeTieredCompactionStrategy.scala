package akka.persistence.cassandra.compaction

import com.typesafe.config.Config

import scala.collection.JavaConverters._

/*
 * Based upon https://github.com/apache/cassandra/blob/cassandra-2.2/src/java/org/apache/cassandra/db/compaction/SizeTieredCompactionStrategy.java
 */
class SizeTieredCompactionStrategy(config: Config) extends BaseCompactionStrategy(config) {
  require(config.hasPath("class") && config.getString("class") == SizeTieredCompactionStrategy.ClassName, s"Config does not specify a ${SizeTieredCompactionStrategy.ClassName}")

  require(
    config.entrySet()
      .asScala
      .map(_.getKey)
      .forall(SizeTieredCompactionStrategy.propertyKeys.contains(_)),
    s"Config contains properties not supported by a ${SizeTieredCompactionStrategy.ClassName}"
  )

  val bucketHigh: Double = if (config.hasPath("bucket_high")) config.getDouble("bucket_high") else 1.5
  val bucketLow: Double = if (config.hasPath("bucket_low")) config.getDouble("bucket_low") else 0.5
  val coldReadsToOmit: Double = if (config.hasPath("cold_reads_to_omit")) config.getDouble("cold_reads_to_omit") else 0.05
  val maxThreshold: Int = if (config.hasPath("max_threshold")) config.getInt("max_threshold") else 32
  val minThreshold: Int = if (config.hasPath("min_threshold")) config.getInt("min_threshold") else 4
  val minSSTableSize: Long = if (config.hasPath("min_sstable_size")) config.getLong("min_sstable_size") else 50

  require(bucketHigh > bucketLow, s"bucket_high must be larger than bucket_low, but was $bucketHigh")
  require(maxThreshold > 0, s"max_threshold must be larger than 0, but was $maxThreshold")
  require(minThreshold > 1, s"min_threshold must be larger than 1, but was $minThreshold")
  require(maxThreshold > minThreshold, s"max_threshold must be larger than min_threshold, but was $maxThreshold")
  require(minSSTableSize > 0, s"min_sstable_size must be larger than 0, but was $minSSTableSize")

  override def asCQL: String =
    s"""{
       |'class' : '${SizeTieredCompactionStrategy.ClassName}',
       |${super.asCQL},
       |'bucket_high' : $bucketHigh,
       |'bucket_low' : $bucketLow,
       |'cold_reads_to_omit' : $coldReadsToOmit,
       |'max_threshold' : $maxThreshold,
       |'min_threshold' : $minThreshold,
       |'min_sstable_size' : $minSSTableSize
       |}
     """.stripMargin.trim
}

object SizeTieredCompactionStrategy extends CassandraCompactionStrategyConfig[SizeTieredCompactionStrategy] {
  override val ClassName: String = "SizeTieredCompactionStrategy"

  override def propertyKeys: List[String] = (
    BaseCompactionStrategy.propertyKeys union List(
      "bucket_high",
      "bucket_low",
      "cold_reads_to_omit",
      "max_threshold",
      "min_threshold",
      "min_sstable_size"
    )
  ).sorted

  override def fromConfig(config: Config): SizeTieredCompactionStrategy = new SizeTieredCompactionStrategy(config)
}