<script setup>
import Login from './components/Login.vue'
import SignUp from './components/SignUp.vue'
import './assets/base.css'
var loginPage = false;
var signUpPage = true;
</script>

<template>
  <head>
  <title>Login</title>
  <!-- <script src="https://cdn.jsdelivr.net/npm/vue@2/dist/vue.js"></script> -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/mdui@0.4.3/dist/css/mdui.min.css"
  crossorigin="anonymous"/>
  <link
      rel="stylesheet"
      href="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css"
      integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh"
      crossorigin="anonymous"
  />
  <title>Home</title>
  <!-- <title>{% block title %}Home{% endblock %}</title> -->
  <!-- TEMPLATE -->
  </head>


  <body class="mdui-theme-layout-dark mdui-theme-accent-orange mdui-theme-primary-orange">
  <div class="mdui-toolbar mdui-color-theme">
  <span style="height: 100%">
      <img src="./assets/logo.svg" style="height: 60%; margin-top: -10%">
  </span>
  <span class="mdui-typo-title"> Dev </span>
  <div class="mdui-toolbar-spacer"></div>
  <!-- NEED TO FIND SECURE WAY OF FINDING OUT OF USER IS AUTHENTICATED -->
  <div v-if="loggedIn">
      <div>
          <button id="open" class="mdui-btn mdui-btn-icon"  mdui-tooltip="{content: 'Home Page'}" onclick="location.href = '/';">
              <i class="mdui-icon material-icons">&#xe88a;</i>
          </button>
          <button id="open" class="mdui-btn mdui-btn-icon" mdui-menu="{target: '#menu'}">
              <i class="mdui-icon material-icons">&#xe7fd;</i>
          </button>
      </div>
      <div>
          <ul class="mdui-menu" id="menu">
              <li class="mdui-menu-item">
                  <a href="/user" class="mdui-ripple">
                      <i class="mdui-menu-item-icon mdui-icon material-icons">&#xe8b8;</i>Preferences
                  </a>
              </li>
              <li class="mdui-menu-item">
                  <a href="javascript:;" class="mdui-ripple">
                      <i class="mdui-menu-item-icon mdui-icon material-icons">&#xe869;</i>Advanced
                  </a>
              </li>
              <li class="mdui-divider"></li>
              <li class="mdui-menu-item">
                  <a href="/logout" class="mdui-ripple">
                      <i class="mdui-menu-item-icon"></i>Sign Out
                  </a>
              </li>
          </ul>
      </div>
  </div>
  <div v-else>
      <div>
          <button id="open" class="mdui-btn mdui-btn-icon"  mdui-tooltip="{content: 'Sign Up'}" onclick="location.href = '/sign-up';">
          <i class="mdui-icon material-icons">&#xe145;</i>
          </button>
          <button id="open" class="mdui-btn mdui-btn-icon"  mdui-tooltip="{content: 'Login'}" onclick="location.href = '/login';">
              <i class="mdui-icon material-icons">&#xe88a;</i>
          </button>
      </div>
  </div>

  </div>
  <!-- {% with messages = get_flashed_messages(with_categories=true) %} {% if
  messages %} {% for category, message in messages %} {% if category ==
  "error" %} -->
  <!-- EMBEDDED IF - TEMPLATE -->
  <!-- DEAL WITH IT LATER -->
  <div v-if="loggedIn">
      <div class="alert alter-dismissable fade show" style="background-color: #F9BA16 !important; color: #0C0E12 !important"  role="alert">
      <h4>Error!</h4>
      <hr/>
      <button type="button" class="close" data-dismiss="alert">
          <span aria-hidden="true">&times;</span>
      </button>
      <p class="mb-0">{{ message }}</p>

      </div>
      <!-- {% else %} -->
      <!-- EMBEDDED IF - TEMPLATE -->
      <div class="alert alter-dismissable fade show" style="background-color: #F9BA16 !important; color: #0C0E12 !important" role="alert">
      <h4>Success!</h4>
      <hr/>
      <button type="button" class="close" data-dismiss="alert">
          <span aria-hidden="true">&times;</span>
      </button>
      <p class="mb-0">{{ message }}</p>
      </div>
  </div>
  <div class="container" v-if="loginPage"><Login/></div>
  <div class="container" v-else-if="signUpPage"><SignUp/></div>
  </body>
</template>

<script>
</script>