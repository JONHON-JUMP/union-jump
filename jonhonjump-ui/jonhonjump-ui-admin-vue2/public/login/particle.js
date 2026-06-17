/* eslint-disable */
(function (global) {
  function ParticleNetwork(canvas, parentEl) {
    this.canvas = canvas
    this.parentEl = parentEl
    this.ctx = canvas.getContext('2d')
    this.config = { particleCount: 80, connectionDistance: 150, particleSize: { min: 1, max: 3 }, speed: { min: 0.15, max: 0.5 }, mouseRadius: 180, mouseForce: 0.02 }
    this.particles = []
    this.mouse = { x: null, y: null, isActive: false }
    this.animationId = null
    this.init()
  }
  ParticleNetwork.prototype.init = function () {
    var self = this
    this.resize()
    this.createParticles()
    this.parentEl.addEventListener('mousemove', function (e) {
      var rect = self.parentEl.getBoundingClientRect()
      self.mouse.x = e.clientX - rect.left
      self.mouse.y = e.clientY - rect.top
      self.mouse.isActive = true
    })
    this.parentEl.addEventListener('mouseleave', function () { self.mouse.isActive = false })
    window.addEventListener('resize', function () { self.resize() })
    this.animate()
  }
  ParticleNetwork.prototype.resize = function () {
    var rect = this.parentEl.getBoundingClientRect()
    var ratio = window.devicePixelRatio || 1
    this.canvas.width = rect.width * ratio
    this.canvas.height = rect.height * ratio
    this.canvas.style.width = rect.width + 'px'
    this.canvas.style.height = rect.height + 'px'
    this.ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
    this.width = rect.width
    this.height = rect.height
  }
  ParticleNetwork.prototype.createParticles = function () {
    this.particles = []
    for (var i = 0; i < this.config.particleCount; i++) {
      this.particles.push({
        x: Math.random() * this.width,
        y: Math.random() * this.height,
        vx: (Math.random() - 0.5) * 0.35,
        vy: (Math.random() - 0.5) * 0.35,
        size: Math.random() * 2 + 1,
        opacity: Math.random() * 0.5 + 0.3,
        pulseSpeed: Math.random() * 0.02 + 0.01,
        pulseOffset: Math.random() * Math.PI * 2,
        type: Math.random() > 0.7 ? 'square' : 'circle'
      })
    }
  }
  ParticleNetwork.prototype.animate = function () {
    var self = this
    this.ctx.clearRect(0, 0, this.width, this.height)
    var time = Date.now() * 0.001
    this.particles.forEach(function (p) {
      p.x += p.vx
      p.y += p.vy
      p.currentOpacity = p.opacity + Math.sin(time * p.pulseSpeed * 60 + p.pulseOffset) * 0.2
      p.vx *= 0.99
      p.vy *= 0.99
      if (p.x < -10) p.x = self.width + 10
      if (p.x > self.width + 10) p.x = -10
      if (p.y < -10) p.y = self.height + 10
      if (p.y > self.height + 10) p.y = -10
    })
    for (var i = 0; i < this.particles.length; i++) {
      for (var j = i + 1; j < this.particles.length; j++) {
        var dx = this.particles[i].x - this.particles[j].x
        var dy = this.particles[i].y - this.particles[j].y
        var dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < this.config.connectionDistance) {
          var opacity = (1 - dist / this.config.connectionDistance) * 0.5
          this.ctx.beginPath()
          this.ctx.strokeStyle = 'rgba(59, 130, 246, ' + opacity + ')'
          this.ctx.moveTo(this.particles[i].x, this.particles[i].y)
          this.ctx.lineTo(this.particles[j].x, this.particles[j].y)
          this.ctx.stroke()
        }
      }
    }
    this.particles.forEach(function (p) {
      var opacity = Math.max(0.1, Math.min(1, p.currentOpacity))
      self.ctx.beginPath()
      self.ctx.fillStyle = 'rgba(96, 165, 250, ' + opacity + ')'
      if (p.type === 'square') {
        var s = p.size * 1.5
        self.ctx.fillRect(p.x - s / 2, p.y - s / 2, s, s)
      } else {
        self.ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
        self.ctx.fill()
      }
    })
    this.animationId = requestAnimationFrame(function () { self.animate() })
  }
  global.ParticleNetwork = ParticleNetwork
})(window)
