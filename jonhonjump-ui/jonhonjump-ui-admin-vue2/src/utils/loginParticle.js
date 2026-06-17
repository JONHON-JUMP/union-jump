export class ParticleNetwork {
  constructor(canvas, parentEl) {
    this.canvas = canvas
    this.parentEl = parentEl
    this.ctx = canvas.getContext('2d')
    this.config = {
      particleCount: 80,
      connectionDistance: 150,
      particleSize: { min: 1, max: 3 },
      speed: { min: 0.15, max: 0.5 },
      mouseRadius: 180,
      mouseForce: 0.02
    }
    this.particles = []
    this.mouse = { x: null, y: null, isActive: false }
    this.animationId = null
    this.onMouseMove = this.handleMouseMove.bind(this)
    this.onMouseLeave = this.handleMouseLeave.bind(this)
    this.onResize = this.handleResize.bind(this)
    this.init()
  }

  init() {
    this.resize()
    this.createParticles()
    this.parentEl.addEventListener('mousemove', this.onMouseMove)
    this.parentEl.addEventListener('mouseleave', this.onMouseLeave)
    window.addEventListener('resize', this.onResize)
    this.animate()
  }

  resize() {
    const rect = this.parentEl.getBoundingClientRect()
    const ratio = window.devicePixelRatio || 1
    this.canvas.width = rect.width * ratio
    this.canvas.height = rect.height * ratio
    this.canvas.style.width = `${rect.width}px`
    this.canvas.style.height = `${rect.height}px`
    this.ctx.setTransform(ratio, 0, 0, ratio, 0, 0)
    this.width = rect.width
    this.height = rect.height
  }

  createParticles() {
    const { particleCount, particleSize, speed } = this.config
    this.particles = []
    for (let i = 0; i < particleCount; i++) {
      this.particles.push({
        x: Math.random() * this.width,
        y: Math.random() * this.height,
        vx: (Math.random() - 0.5) * (speed.max - speed.min) + speed.min * (Math.random() > 0.5 ? 1 : -1),
        vy: (Math.random() - 0.5) * (speed.max - speed.min) + speed.min * (Math.random() > 0.5 ? 1 : -1),
        size: Math.random() * (particleSize.max - particleSize.min) + particleSize.min,
        opacity: Math.random() * 0.5 + 0.3,
        pulseSpeed: Math.random() * 0.02 + 0.01,
        pulseOffset: Math.random() * Math.PI * 2,
        type: Math.random() > 0.7 ? 'square' : 'circle'
      })
    }
  }

  handleMouseMove(e) {
    const rect = this.parentEl.getBoundingClientRect()
    this.mouse.x = e.clientX - rect.left
    this.mouse.y = e.clientY - rect.top
    this.mouse.isActive = true
  }

  handleMouseLeave() {
    this.mouse.isActive = false
  }

  handleResize() {
    this.resize()
    this.particles.forEach(p => {
      if (p.x > this.width) p.x = Math.random() * this.width
      if (p.y > this.height) p.y = Math.random() * this.height
    })
  }

  updateParticles() {
    const { mouseRadius, mouseForce } = this.config
    const time = Date.now() * 0.001
    this.particles.forEach(p => {
      p.x += p.vx
      p.y += p.vy
      p.currentOpacity = p.opacity + Math.sin(time * p.pulseSpeed * 60 + p.pulseOffset) * 0.2
      if (this.mouse.isActive) {
        const dx = this.mouse.x - p.x
        const dy = this.mouse.y - p.y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < mouseRadius) {
          const force = (1 - dist / mouseRadius) * mouseForce
          p.vx += dx * force * 0.01
          p.vy += dy * force * 0.01
        }
      }
      p.vx *= 0.99
      p.vy *= 0.99
      if (p.x < -10) p.x = this.width + 10
      if (p.x > this.width + 10) p.x = -10
      if (p.y < -10) p.y = this.height + 10
      if (p.y > this.height + 10) p.y = -10
    })
  }

  drawConnections() {
    const { connectionDistance } = this.config
    const particles = this.particles
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < connectionDistance) {
          const opacity = (1 - dist / connectionDistance) * 0.5
          this.ctx.beginPath()
          this.ctx.strokeStyle = `rgba(59, 130, 246, ${opacity})`
          this.ctx.lineWidth = 0.6
          this.ctx.moveTo(particles[i].x, particles[i].y)
          this.ctx.lineTo(particles[j].x, particles[j].y)
          this.ctx.stroke()
        }
      }
    }
  }

  drawParticles() {
    this.particles.forEach(p => {
      const opacity = Math.max(0.1, Math.min(1, p.currentOpacity))
      this.ctx.beginPath()
      this.ctx.fillStyle = `rgba(59, 130, 246, ${opacity * 0.15})`
      this.ctx.arc(p.x, p.y, p.size * 4, 0, Math.PI * 2)
      this.ctx.fill()
      if (p.type === 'square') {
        const s = p.size * 1.5
        this.ctx.fillStyle = `rgba(96, 165, 250, ${opacity})`
        this.ctx.fillRect(p.x - s / 2, p.y - s / 2, s, s)
      } else {
        this.ctx.beginPath()
        this.ctx.fillStyle = `rgba(96, 165, 250, ${opacity})`
        this.ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
        this.ctx.fill()
      }
    })
  }

  animate() {
    this.ctx.clearRect(0, 0, this.width, this.height)
    this.updateParticles()
    this.drawConnections()
    this.drawParticles()
    this.animationId = requestAnimationFrame(() => this.animate())
  }

  destroy() {
    cancelAnimationFrame(this.animationId)
    this.parentEl.removeEventListener('mousemove', this.onMouseMove)
    this.parentEl.removeEventListener('mouseleave', this.onMouseLeave)
    window.removeEventListener('resize', this.onResize)
  }
}
