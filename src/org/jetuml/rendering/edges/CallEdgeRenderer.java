/*******************************************************************************
 * JetUML - A desktop application for fast UML diagramming.
 *
 * Copyright (C) 2020, 2021 by McGill University.
 *     
 * See: https://github.com/prmr/JetUML
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *******************************************************************************/
package org.jetuml.rendering.edges;

import java.util.ArrayList;

import org.jetuml.diagram.DiagramElement;
import org.jetuml.diagram.DiagramType;
import org.jetuml.diagram.Edge;
import org.jetuml.diagram.Node;
import org.jetuml.diagram.edges.CallEdge;
import org.jetuml.diagram.edges.ConstructorEdge;
import org.jetuml.diagram.nodes.CallNode;
import org.jetuml.geom.Conversions;
import org.jetuml.geom.Dimension;
import org.jetuml.geom.Direction;
import org.jetuml.geom.Line;
import org.jetuml.geom.Point;
import org.jetuml.geom.Rectangle;
import org.jetuml.rendering.ArrowHead;
import org.jetuml.rendering.ArrowHeadViewer;
import org.jetuml.rendering.DiagramRenderer;
import org.jetuml.rendering.LineStyle;
import org.jetuml.rendering.StringRenderer;
import org.jetuml.rendering.ToolGraphics;
import org.jetuml.rendering.StringRenderer.Alignment;
import org.jetuml.rendering.StringRenderer.TextDecoration;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape;

/**
 * A viewer to show call edges in a sequence diagrams. These are labeled
 * edges that are either straight or self-edges, in a solid line, and 
 * have either a V or half-V arrow head.
 */
public final class CallEdgeRenderer extends AbstractEdgeRenderer
{	
	private static final StringRenderer CENTERED_STRING_VIEWER = StringRenderer.get(Alignment.CENTER_CENTER, TextDecoration.PADDED);
	private static final StringRenderer LEFT_JUSTIFIED_STRING_VIEWER = StringRenderer.get(Alignment.TOP_LEFT, TextDecoration.PADDED);

	private static final int SHIFT = 5;
	
	public CallEdgeRenderer(DiagramRenderer pParent)
	{
		super(pParent);
	}
	
	@Override
	protected Shape getShape(Edge pEdge)
	{
		Point[] points = getPoints(pEdge);
		Path path = new Path();
		Point point = points[points.length - 1];
		MoveTo moveTo = new MoveTo(point.getX(), point.getY());
		path.getElements().add(moveTo);
		for(int i = points.length - 2; i >= 0; i--)
		{
			point = points[i];
			LineTo lineTo = new LineTo(point.getX(), point.getY());
			path.getElements().add(lineTo);
		}
		return path;
	}
	
	@Override
	public Line getConnectionPoints(Edge pEdge)
	{
		Point[] points = getPoints(pEdge);
		assert points.length >= 2;
		return new Line(points[0], points[points.length-1]);
	}
	
	private static ArrowHeadViewer getArrowHeadView(CallEdge pEdge)
	{
		if(pEdge.isSignal())
		{
			return ArrowHead.HALF_V.view();
		}
		else
		{
			return ArrowHead.V.view();
		}
	}
	
	@Override
	public Rectangle getBounds(DiagramElement pElement)
	{
		Rectangle bounds = super.getBounds(pElement);
		Edge edge = (Edge) pElement;
		Line connectionPoints = getConnectionPoints(edge);
		bounds = bounds.add(Conversions.toRectangle(getArrowHeadView((CallEdge)edge).getPath(connectionPoints.getPoint1(), 
					connectionPoints.getPoint2()).getBoundsInLocal()));
		final String label = ((CallEdge)edge).getMiddleLabel();
		if( label.length() > 0 )
		{
			bounds = bounds.add(getStringBounds((CallEdge)edge));
		}
		return bounds;
	}

	@Override
	public void draw(DiagramElement pElement, GraphicsContext pGraphics)
	{
		Edge edge = (Edge) pElement;
		ToolGraphics.strokeSharpPath(pGraphics, (Path) getShape(edge), LineStyle.SOLID);
		
		Point[] points = getPoints(edge); // TODO already called by getShape(), find a way to avoid having to do 2 calls.
		getArrowHeadView((CallEdge)edge).draw(pGraphics, points[points.length - 2], points[points.length - 1]);
		String label = ((CallEdge)edge).getMiddleLabel();
		if( label.length() > 0 )
		{
			drawLabel((CallEdge)edge, pGraphics, label);
		}
	}
	
	private Rectangle getStringBounds(CallEdge pEdge)
	{
		assert pEdge != null;
		final String label = pEdge.getMiddleLabel();
		if( pEdge.isSelfEdge() )
		{
			Dimension dimensions = LEFT_JUSTIFIED_STRING_VIEWER.getDimension(label);
			Point[] points = getPoints(pEdge);
			int heightDelta = (points[2].getY() -  points[1].getY() - dimensions.height())/2 + SHIFT;
			return new Rectangle(points[1].getX(), points[1].getY() + heightDelta, dimensions.width() , dimensions.height());
		}
		else
		{
			Dimension dimensions = CENTERED_STRING_VIEWER.getDimension(label);
			Point center = getConnectionPoints(pEdge).spanning().getCenter();
			return new Rectangle(center.getX() - dimensions.width()/2, 
					center.getY() - dimensions.height() + SHIFT, dimensions.width(), dimensions.height());
		}
	}

	private void drawLabel(CallEdge pEdge, GraphicsContext pGraphics, String pLabel)
	{
		if( pEdge.isSelfEdge() )
		{
			LEFT_JUSTIFIED_STRING_VIEWER.draw(pLabel, pGraphics, getStringBounds(pEdge));
		}
		else
		{
			CENTERED_STRING_VIEWER.draw(pLabel, pGraphics, getStringBounds(pEdge));
		}
	}
	
	/* Gets the points on a segmented path */ 
	private Point[] getPoints(Edge pEdge)
	{
		ArrayList<Point> points = new ArrayList<>();
		Node endNode = pEdge.getEnd();
		if( pEdge.getClass() == ConstructorEdge.class )
		{
			endNode = pEdge.getEnd().getParent();
		}
		Rectangle start = parent().getBounds(pEdge.getStart());	
		Rectangle end = parent().getBounds(endNode);
		if( ((CallEdge)pEdge).isSelfEdge() )
		{
			Point p = new Point(start.getMaxX(), end.getY() - CallNode.CALL_YGAP / 2);
			Point q = new Point(end.getMaxX(), end.getY());
			Point s = new Point(q.getX() + end.getWidth(), q.getY());
			Point r = new Point(s.getX(), p.getY());

			points.add(p);
			points.add(r);
			points.add(s);
			points.add(q);
		}
		else     
		{
			Direction direction = Direction.WEST;
			if( start.getX() > end.getX() )
			{
				direction = Direction.EAST;
			}
			Point endPoint = parent().getConnectionPoints(endNode, direction);
         
			if(start.getCenter().getX() < endPoint.getX())
			{
				points.add(new Point(start.getMaxX(), endPoint.getY()));
			}
			else
			{
				points.add(new Point(start.getX(), endPoint.getY()));
			}
			points.add(endPoint);
		}
		return points.toArray(new Point[points.size()]);
	}
	
	@Override
	public Canvas createIcon(DiagramType pDiagramType, DiagramElement pElement)
	{
		final float scale = 0.6f;
		final int offset = 15;
		Canvas canvas = new Canvas(BUTTON_SIZE, BUTTON_SIZE);
		GraphicsContext graphics = canvas.getGraphicsContext2D();
		canvas.getGraphicsContext2D().scale(scale, scale);
		Path path = new Path();
		path.getElements().addAll(new MoveTo(1, offset), new LineTo(BUTTON_SIZE*(1/scale)-1, offset));
		ToolGraphics.strokeSharpPath(graphics, path, LineStyle.SOLID);
		ArrowHead.V.view().draw(graphics, new Point(1, offset), new Point((int)(BUTTON_SIZE*(1/scale)-1), offset));
		return canvas;
	}
}
