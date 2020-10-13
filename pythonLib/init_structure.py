class Init_structure:
    def __init__(self, ident, x, y, angle, length=1.5, color=BLUE):
        self.ident = ident
        self.x = x
        self.y = y
        self.angle = angle
        self.length = length
        self.color = color

    def build(self):
        line = Line()
        line.set_length(self.length)
        line.set_angle(self.angle)
        line.to_edge(np.array([self.x, self.y, 0]))
        label = TextMobject(self.ident)
        label.next_to(line, DOWN, SMALL_BUFF)
        group = VGroup(label, line)
        return group
