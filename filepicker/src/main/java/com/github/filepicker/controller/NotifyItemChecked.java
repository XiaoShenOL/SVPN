/*
 * Copyright (C) 2016 Angad Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.filepicker.controller;

/**<p>
 * Created by Angad Singh on 11-07-2016.
 * </p>
 */

/**
 * Interface definition for a callback to be invoked
 * when a checkbox is checked.
 */
public interface NotifyItemChecked {

    /**
     * Called when a checkbox is checked.
     */
    void notifyCheckBoxIsClicked();
}
