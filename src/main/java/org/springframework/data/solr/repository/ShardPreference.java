package org.springframework.data.solr.repository;

import java.lang.annotation.*;
import org.apache.solr.cluster.Replica;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ShardPreference {
  String value() default "";

  String[] location() default {};

  Replica.ReplicaType[] replicaType() default {};

  LeaderPreference preferLeader() default LeaderPreference.NONE;

  enum LeaderPreference {
    NONE, ALWAYS, NEVER;
  }

}
