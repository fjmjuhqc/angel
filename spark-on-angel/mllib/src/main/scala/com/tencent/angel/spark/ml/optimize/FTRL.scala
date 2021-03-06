/*
 * Tencent is pleased to support the open source community by making Angel available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.angel.spark.ml.optimize

object FTRL {

  // compute the increment for z and n model for one instance
  def trainByInstance(data: (Double, Array[(Long, Double)]),
                      localZ: Map[Long, Double],
                      localN: Map[Long, Double],
                      alpha: Double,
                      beta: Double,
                      lambda1: Double,
                      lambda2: Double,
                      getGradLoss:(Map[Long, Double], Double, Array[(Long, Double)]) => Map[Long, Double]
                     ): (Map[Long, Double], Map[Long, Double]) = {

    val label = data._1
    val feature = data._2

    // init w which is the weight of the model
    var localW: Map[Long, Double] = Map()

    // update the w
    feature.foreach{ case(fId, value) =>
      val zVal = localZ.getOrElse(fId, 0.0)
      val nVal = localN.getOrElse(fId, 0.0)
      // w_local的更新
      localW += (fId -> updateWeight(zVal, nVal, alpha, beta, lambda1, lambda2))
    }

    // compute the new gradient
    val newGradient = getGradLoss(localW, label, feature)

    var gOnId = 0.0
    var dOnId = 0.0

    // update z and n in all dimension
    var incrementZ: Map[Long, Double] = Map()
    var incrementN: Map[Long, Double] = Map()

    feature.foreach{ case(fId, value) =>

      val nVal = localN.getOrElse(fId, 0.0)

      // G(t):第t次迭代中损失函数梯度，g(t)表示某一维度上的梯度
      gOnId = newGradient.getOrElse(fId, 0.0)
      // delta(s),n_val初始为0，z(i)初始为0
      dOnId = 1.0 / alpha * (Math.sqrt(nVal + gOnId * gOnId) - Math.sqrt(nVal))

      incrementZ += (fId -> (gOnId - dOnId * localW.getOrElse(fId, 0.0)))
      incrementN += (fId -> gOnId * gOnId)
    }

    (incrementZ, incrementN)
  }

  // compute new weight
  def updateWeight(zOnId: Double,
                   nOnId: Double,
                   alpha: Double,
                   beta: Double,
                   lambda1: Double,
                   lambda2: Double): Double = {
    if (Math.abs(zOnId) <= lambda1)
      0.0
    else
      (-1) * (1.0 / (lambda2 + (beta + Math.sqrt(nOnId)) / alpha)) * (zOnId - Math.signum(zOnId).toInt * lambda1)
  }

}