// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/* Show picture of dog */
function showPic(){
    document.getElementById("show").classList.add("hidden");
    document.getElementById("pic").classList.remove("hidden");
}

/* Switch to new page */
function changePage(newPage){
    location.href = newPage;
}

/* Display comments */
function getComments() {
    // fetch('/data').then(response => response.text()).then((message) => {
    //   document.getElementById('show-comments').innerText = message;
    document.getElementById('show-comments').innerHTML = "";
    let num = document.getElementById("num-comments").value;
    num = parseInt(num);
    if (!isNaN(num)) {
        fetch('/data').then(response => response.json()).then((comments) => {
            const commentList = document.getElementById('show-comments');
            for (let i=0; i<num; i++) {
                commentList.appendChild(createListElement(comments[i]));
            }
        });
    }
}

/* Creates an <li> element containing text (from subtraction-game). */
function createListElement(text) {
  const liElement = document.createElement('li');
  liElement.innerText = text;
  return liElement;
}