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
    document.getElementById('show-comments').innerHTML = "";
    let num = document.getElementById("num-show").value;
    num = parseInt(num);
    if (!(isNaN(num) || num == 0)) {
        fetch('/data')
        .then(response => response.json())
        .then((response) => {
            return JSON.parse(response.comments); // Get comments as array.
        })
        .then((comments) => {
            const commentList = document.getElementById('show-comments');
            for (let i=0; i<num; i++) {
                commentList.appendChild(createListElement(comments[i]));
            }
        });
    }
}

/* Display images */
function getImages() {
    document.getElementById('show-images').innerHTML = "";
    let num = document.getElementById("num-show").value;
    num = parseInt(num);
    if (!(isNaN(num) || num == 0)) {
        fetch('/data')
        .then(response => response.json())
        .then((response) => {
            return JSON.parse(response.url); // Get images as array.
        })
        .then((images) => {
            const imageList = document.getElementById('show-images');
            for (let i=0; i<num; i++) {
                imageList.appendChild(createListImageElement(images[i]));
            }
        });
    }
}

/* Delete comments */
function deleteComments() {
    const request = new Request('/delete-data', {method: 'POST'});
    fetch(request).then(fetch('/data')).then(response => response.json()).then((comments) => {
        const commentList = document.getElementById('show-comments');
        commentList.innerHTML = comments;
    });
}

/* Creates an <li> element containing text. */
function createListElement(text) {
  const liElement = document.createElement('li');
  liElement.innerText = text;
  return liElement;
}

/* Creates an <li> element containing an image. */
function createListImageElement(src) {
  const liElement = document.createElement('li');
  const imgElement = document.createElement('img');
  imgElement.src = src;
  liElement.appendChild(imgElement);
  return liElement;
}

/* Generates upload URL for form. */
function fetchBlobstoreUrl() {
  fetch('/blobstore-upload-url') // Generate upload URL.
      .then(response => response.text())
      .then((imageUploadUrl) => {
        const imageForm = document.getElementById("image-form");
        imageForm.action = imageUploadUrl;
      });
}