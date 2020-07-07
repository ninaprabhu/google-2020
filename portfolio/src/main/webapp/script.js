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
    let num = document.getElementById("num-comments").value;
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
    document.getElementById('show-comments').innerHTML = "";
    let num = document.getElementById("num-comments").value;
    num = parseInt(num);
    if (!(isNaN(num) || num == 0)) {
        fetch('/data')
        .then(response => response.json())
        .then((response) => {
            return JSON.parse(response.url); // Get comments as array.
        })
        .then((images) => {
            const imageList = document.getElementById('show-comments');
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

/* Creates an <li> element containing text. */
function createListImageElement(src) {
  const liElement = document.createElement('li');
  liElement.innerText = "<img src="+src+"></img>";
  return liElement;
}

function fetchBlobstoreUrlAndShowForm() {
//   const request = new Request('/data', {method: 'POST'});
  fetch('/blobstore-upload-url') // Generate upload URL.
      .then(response => response.json())
        .then((response) => {
        return JSON.parse(response.url); // Get image upload URL.
    })
      .then((imageUploadUrl) => {
        const imageForm = document.getElementById("image-form");
        imageForm.action = imageUploadUrl;
        // imageForm.classList.remove('hidden');
      });
    //   .then(fetch(request));
}


// function showImage() {
//   fetch('/my-form-handler')
//     .then(response => response.json())
//     // .then((response) => {
//     //     return JSON.parse(response.url); // Get image URL.
//     // })
//     .then((imageUploadUrl) => {
//         console.log(imageUploadUrl);
//         const showImage = document.getElementById("show-image");
//         showImage.src = imageUploadUrl;
//         showImage.classList.remove('hidden');
//     });
// }