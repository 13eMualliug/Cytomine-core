/*
 * Copyright (c) 2009-2019. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var Processing = Processing || {};

Processing.ColorChannel = $.extend({}, Processing.Utils,
    {
        RED: 0,
        GREEN: 1,
        BLUE: 2,
        ALPHA: 3,
        process: function (params) {
            console.log("Thresholding...");
            var canvas = params.canvas;
            var channel = params.channel;
            var d = canvas.data;
            for (var i = 0; i < d.length; i += 4) {
                var v = d[i + channel];
                d[i] = d[i + 1] = d[i + 2] = v
            }
            return canvas;
        }
    });